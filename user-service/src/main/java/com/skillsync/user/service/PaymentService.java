package com.skillsync.user.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.skillsync.user.config.RabbitMQConfig;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.Payment;
import com.skillsync.user.enums.PaymentStatus;
import com.skillsync.user.enums.PaymentType;
import com.skillsync.user.enums.ReferenceType;
import com.skillsync.user.event.PaymentCompletedEvent;
import com.skillsync.user.exception.PaymentException;
import com.skillsync.user.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment service handling Razorpay order creation, verification, and
 * delegating post-payment business actions to {@link PaymentSagaOrchestrator}.
 *
 * <h3>Security:</h3>
 * <ul>
 *   <li>userId is ALWAYS extracted from JWT header (X-User-Id) — never from request body/params</li>
 *   <li>Every payment operation validates ownership against the authenticated user</li>
 * </ul>
 *
 * <h3>Idempotency:</h3>
 * <ul>
 *   <li>Duplicate order creation is prevented per reference mapping</li>
 *   <li>Verify endpoint is safe to retry — returns current state if already processed</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final int FIXED_AMOUNT_PAISE = 900; // ₹9 in paise
    private static final String CURRENCY = "INR";

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final PaymentSagaOrchestrator sagaOrchestrator;
    private final RabbitTemplate rabbitTemplate;

    @Value("${razorpay.api.key}")
    private String razorpayKeyId;

    @Value("${razorpay.api.secret}")
    private String razorpayKeySecret;

    // ═════════════════════════════════════════════
    //  STEP 1: Create Razorpay Order
    // ═════════════════════════════════════════════

    @Transactional
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        log.info("[PAYMENT] Creating order: userId={}, type={}, referenceId={}, referenceType={}",
                userId, request.type(), request.referenceId(), request.referenceType());

        // Validate reference mapping consistency
        validateReferenceMapping(request.type(), request.referenceType());

        // Prevent duplicate payments for the same reference
        preventDuplicatePayment(userId, request);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", FIXED_AMOUNT_PAISE);
            orderRequest.put("currency", CURRENCY);
            orderRequest.put("receipt", "receipt_" + userId + "_" + System.currentTimeMillis());

            JSONObject notes = new JSONObject();
            notes.put("userId", userId);
            notes.put("type", request.type().name());
            notes.put("referenceId", request.referenceId());
            notes.put("referenceType", request.referenceType().name());
            orderRequest.put("notes", notes);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            // Persist payment record with reference mapping
            Payment payment = Payment.builder()
                    .userId(userId)
                    .type(request.type())
                    .amount(FIXED_AMOUNT_PAISE)
                    .razorpayOrderId(orderId)
                    .status(PaymentStatus.CREATED)
                    .referenceId(request.referenceId())
                    .referenceType(request.referenceType())
                    .build();
            paymentRepository.save(payment);

            log.info("[PAYMENT] Order created: orderId={}, userId={}, type={}, referenceId={}",
                    orderId, userId, request.type(), request.referenceId());

            return new CreateOrderResponse(
                    orderId,
                    FIXED_AMOUNT_PAISE,
                    CURRENCY,
                    PaymentStatus.CREATED.name(),
                    razorpayKeyId
            );

        } catch (RazorpayException e) {
            log.error("[PAYMENT] Razorpay order creation failed: userId={}, error={}",
                    userId, e.getMessage(), e);
            throw new PaymentException("ORDER_CREATION_FAILED",
                    "Failed to create payment order: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════
    //  STEP 2: Verify Payment + Trigger Saga
    // ═════════════════════════════════════════════

    @Transactional
    public PaymentResponse verifyPayment(Long userId, VerifyPaymentRequest request) {
        log.info("[PAYMENT] Verifying payment: userId={}, orderId={}", userId, request.razorpayOrderId());

        // 1. Find the payment record
        Payment payment = paymentRepository.findByRazorpayOrderId(request.razorpayOrderId())
                .orElseThrow(() -> new PaymentException("ORDER_NOT_FOUND",
                        "Payment order not found: " + request.razorpayOrderId(),
                        HttpStatus.NOT_FOUND));

        // 2. Security: ensure payment belongs to the authenticated user
        if (!payment.getUserId().equals(userId)) {
            log.warn("[SECURITY] User {} attempted to verify payment belonging to user {}",
                    userId, payment.getUserId());
            throw new PaymentException("UNAUTHORIZED_ACCESS",
                    "Payment does not belong to the authenticated user",
                    HttpStatus.FORBIDDEN);
        }

        // 3. Idempotency: if already fully processed, return current state
        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.COMPENSATED) {
            log.info("[PAYMENT] Payment already processed (status={}): orderId={}",
                    payment.getStatus(), request.razorpayOrderId());
            return mapToResponse(payment);
        }

        // 4. Prevent re-processing of failed payments
        if (payment.getStatus() == PaymentStatus.FAILED) {
            throw new PaymentException("PAYMENT_ALREADY_FAILED",
                    "Payment was previously marked as failed. Create a new order.");
        }

        // 5. If saga is already in progress (SUCCESS_PENDING), return current state
        if (payment.getStatus() == PaymentStatus.SUCCESS_PENDING) {
            log.warn("[PAYMENT] Payment is already in saga processing: orderId={}",
                    request.razorpayOrderId());
            return mapToResponse(payment);
        }

        // 6. Verify Razorpay signature
        verifyRazorpaySignature(payment, request);

        // 7. Validate amount
        if (payment.getAmount() != FIXED_AMOUNT_PAISE) {
            markPaymentFailed(payment, "Amount mismatch: expected " + FIXED_AMOUNT_PAISE
                    + " but found " + payment.getAmount());
            throw new PaymentException("AMOUNT_MISMATCH",
                    "Payment amount verification failed");
        }

        // 8. Mark payment as VERIFIED (signature + amount validated)
        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        paymentRepository.save(payment);

        log.info("[PAYMENT] Payment verified: orderId={}, paymentId={}, userId={}, type={}",
                request.razorpayOrderId(), request.razorpayPaymentId(),
                userId, payment.getType());

        // 9. Trigger Saga Orchestration (outside the verification transaction)
        sagaOrchestrator.executeSaga(payment);

        // 10. Re-fetch to get latest status after saga
        payment = paymentRepository.findByRazorpayOrderId(request.razorpayOrderId())
                .orElse(payment);

        return mapToResponse(payment);
    }

    // ═════════════════════════════════════════════
    //  Query Methods (Secure)
    // ═════════════════════════════════════════════

    /**
     * Get all payments for the authenticated user.
     * userId is always from JWT — never from request params.
     */
    public List<PaymentResponse> getUserPayments(Long userId) {
        log.debug("[PAYMENT] Fetching payments for userId={}", userId);
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific payment by Razorpay order ID with ownership check.
     */
    public PaymentResponse getPaymentByOrderId(Long userId, String orderId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new PaymentException("ORDER_NOT_FOUND",
                        "Payment order not found: " + orderId, HttpStatus.NOT_FOUND));

        // Security: verify ownership
        if (!payment.getUserId().equals(userId)) {
            throw new PaymentException("UNAUTHORIZED_ACCESS",
                    "Payment does not belong to the authenticated user",
                    HttpStatus.FORBIDDEN);
        }

        return mapToResponse(payment);
    }

    /**
     * Internal check: verify if a user has a successful payment of a given type.
     * Used by inter-service calls (e.g., session-service via Feign).
     * Only accessible via internal service-to-service requests (X-User-Id header from gateway).
     */
    public boolean hasSuccessfulPayment(Long userId, PaymentType type) {
        return !paymentRepository
                .findByUserIdAndTypeAndStatus(userId, type, PaymentStatus.SUCCESS)
                .isEmpty();
    }

    // ═════════════════════════════════════════════
    //  Validation Helpers
    // ═════════════════════════════════════════════

    /**
     * Ensure PaymentType and ReferenceType are consistent.
     */
    private void validateReferenceMapping(PaymentType type, ReferenceType referenceType) {
        boolean valid = switch (type) {
            case MENTOR_FEE -> referenceType == ReferenceType.MENTOR_ONBOARDING;
            case SESSION_BOOKING -> referenceType == ReferenceType.SESSION_BOOKING;
        };

        if (!valid) {
            throw new PaymentException("INVALID_REFERENCE",
                    String.format("PaymentType %s is not compatible with ReferenceType %s",
                            type, referenceType));
        }
    }

    /**
     * Prevent creating duplicate payments for the same business reference.
     */
    private void preventDuplicatePayment(Long userId, CreateOrderRequest request) {
        // For MENTOR_FEE: only one successful/pending payment per user
        if (request.type() == PaymentType.MENTOR_FEE) {
            List<Payment> existing = paymentRepository
                    .findByUserIdAndTypeAndStatus(userId, PaymentType.MENTOR_FEE, PaymentStatus.SUCCESS);
            if (!existing.isEmpty()) {
                throw new PaymentException("DUPLICATE_PAYMENT",
                        "Mentor onboarding fee has already been paid for userId=" + userId,
                        HttpStatus.CONFLICT);
            }
        }

        // Check for active (non-terminal) payments on same reference
        List<PaymentStatus> activeStatuses = List.of(
                PaymentStatus.CREATED, PaymentStatus.VERIFIED, PaymentStatus.SUCCESS_PENDING);
        List<Payment> activePayments = paymentRepository
                .findByReferenceIdAndReferenceTypeAndStatusIn(
                        request.referenceId(), request.referenceType(), activeStatuses);
        if (!activePayments.isEmpty()) {
            throw new PaymentException("DUPLICATE_PAYMENT",
                    "An active payment already exists for this reference. " +
                            "Complete or cancel the existing payment first.",
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * Verify Razorpay payment signature.
     */
    private void verifyRazorpaySignature(Payment payment, VerifyPaymentRequest request) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.razorpayOrderId());
            attributes.put("razorpay_payment_id", request.razorpayPaymentId());
            attributes.put("razorpay_signature", request.razorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
            if (!isValid) {
                markPaymentFailed(payment, "Razorpay signature verification failed");
                throw new PaymentException("SIGNATURE_INVALID",
                        "Payment signature verification failed");
            }
        } catch (RazorpayException e) {
            markPaymentFailed(payment, "Signature verification exception: " + e.getMessage());
            throw new PaymentException("SIGNATURE_INVALID",
                    "Payment signature verification failed: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════
    //  Private Helpers
    // ═════════════════════════════════════════════

    private void markPaymentFailed(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setCompensationReason(reason);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        log.warn("[PAYMENT] Payment marked FAILED: orderId={}, reason={}",
                payment.getRazorpayOrderId(), reason);

        // Publish payment.failed notification event
        publishPaymentFailedEvent(payment);
    }

    private void publishPaymentFailedEvent(Payment payment) {
        try {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    payment.getUserId(),
                    payment.getRazorpayOrderId(),
                    payment.getType().name(),
                    PaymentStatus.FAILED.name(),
                    payment.getAmount(),
                    payment.getReferenceId(),
                    payment.getReferenceType() != null ? payment.getReferenceType().name() : null,
                    payment.getCompensationReason()
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENT_EXCHANGE, "payment.failed", event);
            log.info("[NOTIFICATION] Published payment.failed event for orderId={}, userId={}",
                    payment.getRazorpayOrderId(), payment.getUserId());
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to publish payment.failed event for orderId={}",
                    payment.getRazorpayOrderId(), e);
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getType().name(),
                payment.getAmount(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getStatus().name(),
                payment.getReferenceId(),
                payment.getReferenceType() != null ? payment.getReferenceType().name() : null,
                payment.getCompensationReason(),
                payment.getCreatedAt(),
                payment.getCompletedAt()
        );
    }
}
