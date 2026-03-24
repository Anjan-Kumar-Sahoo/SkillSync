package com.skillsync.user.controller;

import com.skillsync.user.dto.*;
import com.skillsync.user.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Payment REST controller.
 *
 * <h3>Security:</h3>
 * <ul>
 *   <li>userId is ALWAYS extracted from the X-User-Id header (set by API Gateway from JWT)</li>
 *   <li>No endpoint accepts userId as a request parameter or in the body</li>
 *   <li>All operations are scoped to the authenticated user</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * STEP 1 — Create a Razorpay order.
     * Returns orderId and Razorpay key for frontend checkout.
     *
     * @param userId authenticated user ID from JWT (via gateway header)
     */
    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = paymentService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * STEP 2 — Verify payment after frontend checkout.
     * Validates Razorpay signature, triggers saga orchestration for business actions.
     *
     * @param userId authenticated user ID from JWT (via gateway header)
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody VerifyPaymentRequest request) {
        PaymentResponse response = paymentService.verifyPayment(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for the authenticated user (ordered newest first).
     *
     * @param userId authenticated user ID from JWT (via gateway header)
     */
    @GetMapping("/my-payments")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(paymentService.getUserPayments(userId));
    }

    /**
     * Get a specific payment by Razorpay order ID.
     * Ownership is validated — only the payment owner can access this.
     *
     * @param userId authenticated user ID from JWT (via gateway header)
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(userId, orderId));
    }

    /**
     * Internal API: Check if a user has a successful payment for a given type.
     * Used by inter-service calls (e.g., session-service via Feign).
     *
     * <p>Note: This endpoint uses the authenticated userId from the header,
     * not from query params. For service-to-service calls, the calling service
     * must forward the X-User-Id header.</p>
     *
     * @param userId authenticated user ID from JWT (via gateway header)
     */
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkPaymentStatus(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam com.skillsync.user.enums.PaymentType type) {
        return ResponseEntity.ok(paymentService.hasSuccessfulPayment(userId, type));
    }
}
