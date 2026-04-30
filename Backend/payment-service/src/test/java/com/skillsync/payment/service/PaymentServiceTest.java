package com.skillsync.payment.service;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.skillsync.payment.dto.*;
import com.skillsync.payment.entity.Payment;
import com.skillsync.payment.enums.PaymentStatus;
import com.skillsync.payment.enums.PaymentType;
import com.skillsync.payment.enums.ReferenceType;
import com.skillsync.payment.exception.PaymentException;
import com.skillsync.payment.repository.PaymentRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock private RazorpayClient razorpayClient;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentSagaOrchestrator sagaOrchestrator;
    @Mock private OutboxEventService outboxEventService;
    
    @InjectMocks private PaymentService paymentService;

    @Mock private OrderClient orderClient;
    @Mock private Order razorpayOrder;

    private static final Long USER_ID = 1L;
    private static final String ORDER_ID = "order_123";
    private static final String PAYMENT_ID = "pay_123";
    private static final String SIGNATURE = "sig_123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "test_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_secret");
        
        // Manual field injection for RazorpayClient mock
        try {
            razorpayClient.orders = orderClient;
        } catch (Exception ignored) {}
    }

    @Nested
    @DisplayName("createOrder() tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Success: Valid amount")
        void createOrder_Success() throws RazorpayException {
            CreateOrderRequest request = new CreateOrderRequest(PaymentType.SESSION_BOOKING, 101L, ReferenceType.SESSION_BOOKING, 500);
            
            when(paymentRepository.findByReferenceIdAndReferenceTypeAndStatusIn(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(orderClient.create(any(JSONObject.class))).thenReturn(razorpayOrder);
            when(razorpayOrder.get("id")).thenReturn(ORDER_ID);

            CreateOrderResponse response = paymentService.createOrder(USER_ID, request);

            assertThat(response.orderId()).isEqualTo(ORDER_ID);
            assertThat(response.amount()).isEqualTo(50000); // 500 * 100
            
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getAmount()).isEqualTo(50000);
        }

        @Test
        @DisplayName("Success: Null amount falls back to default")
        void createOrder_NullAmount_Fallback() throws RazorpayException {
            CreateOrderRequest request = new CreateOrderRequest(PaymentType.SESSION_BOOKING, 101L, ReferenceType.SESSION_BOOKING, null);
            
            when(paymentRepository.findByReferenceIdAndReferenceTypeAndStatusIn(anyLong(), any(), any())).thenReturn(Collections.emptyList());
            when(orderClient.create(any(JSONObject.class))).thenReturn(razorpayOrder);
            when(razorpayOrder.get("id")).thenReturn(ORDER_ID);

            CreateOrderResponse response = paymentService.createOrder(USER_ID, request);

            assertThat(response.amount()).isEqualTo(900); // DEFAULT_AMOUNT_PAISE
        }

        @Test
        @DisplayName("Failure: Razorpay exception")
        void createOrder_RazorpayException() throws RazorpayException {
            CreateOrderRequest request = new CreateOrderRequest(PaymentType.SESSION_BOOKING, 101L, ReferenceType.SESSION_BOOKING, 500);
            when(orderClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("RZP Fail"));

            assertThatThrownBy(() -> paymentService.createOrder(USER_ID, request))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "ORDER_CREATION_FAILED");
        }

        @Test
        @DisplayName("Failure: Duplicate active payment")
        void createOrder_Duplicate() {
            CreateOrderRequest request = new CreateOrderRequest(PaymentType.SESSION_BOOKING, 101L, ReferenceType.SESSION_BOOKING, 500);
            when(paymentRepository.findByReferenceIdAndReferenceTypeAndStatusIn(anyLong(), any(), any()))
                    .thenReturn(List.of(new Payment()));

            assertThatThrownBy(() -> paymentService.createOrder(USER_ID, request))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "DUPLICATE_PAYMENT");
        }
    }

    @Nested
    @DisplayName("verifyPayment() tests")
    class VerifyPaymentTests {

        private VerifyPaymentRequest request;
        private Payment payment;

        @BeforeEach
        void setup() {
            request = new VerifyPaymentRequest(ORDER_ID, PAYMENT_ID, SIGNATURE);
            payment = Payment.builder()
                    .userId(USER_ID)
                    .razorpayOrderId(ORDER_ID)
                    .amount(50000)
                    .status(PaymentStatus.CREATED)
                    .type(PaymentType.SESSION_BOOKING)
                    .referenceId(101L)
                    .referenceType(ReferenceType.SESSION_BOOKING)
                    .build();
        }

        @Test
        @DisplayName("Success: Happy path")
        void verify_Success() {
            when(paymentRepository.findByRazorpayOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
            
            try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
                mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString())).thenReturn(true);
                
                PaymentResponse response = paymentService.verifyPayment(USER_ID, request);

                assertThat(response.status()).isEqualTo(PaymentStatus.VERIFIED.name());
                verify(sagaOrchestrator).executeSaga(any(Payment.class));
            }
        }

        @Test
        @DisplayName("Idempotency: Already SUCCESS returns immediately")
        void verify_AlreadySuccess() {
            payment.setStatus(PaymentStatus.SUCCESS);
            when(paymentRepository.findByRazorpayOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.verifyPayment(USER_ID, request);

            assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS.name());
            verifyNoInteractions(sagaOrchestrator);
        }

        @Test
        @DisplayName("Idempotency: Already SUCCESS_PENDING returns immediately")
        void verify_AlreadyPending() {
            payment.setStatus(PaymentStatus.SUCCESS_PENDING);
            when(paymentRepository.findByRazorpayOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.verifyPayment(USER_ID, request);

            assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS_PENDING.name());
            verifyNoInteractions(sagaOrchestrator);
        }

        @Test
        @DisplayName("Failure: Payment previously FAILED")
        void verify_AlreadyFailed() {
            payment.setStatus(PaymentStatus.FAILED);
            when(paymentRepository.findByRazorpayOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.verifyPayment(USER_ID, request))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_ALREADY_FAILED");
        }

        @Test
        @DisplayName("Failure: Unauthorized user")
        void verify_Unauthorized() {
            when(paymentRepository.findByRazorpayOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.verifyPayment(999L, request))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "UNAUTHORIZED_ACCESS");
        }

        @Test
        @DisplayName("Failure: Invalid signature")
        void verify_InvalidSignature() {
            when(paymentRepository.findByRazorpayOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
            
            try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
                mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString())).thenReturn(false);
                
                assertThatThrownBy(() -> paymentService.verifyPayment(USER_ID, request))
                        .isInstanceOf(PaymentException.class)
                        .hasFieldOrPropertyWithValue("errorCode", "SIGNATURE_INVALID");
                
                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            }
        }

        @Test
        @DisplayName("Failure: Razorpay exception during signature verification")
        void verify_SignatureException() {
            when(paymentRepository.findByRazorpayOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
            
            try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
                mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString()))
                        .thenThrow(new RazorpayException("Verification Error"));
                
                assertThatThrownBy(() -> paymentService.verifyPayment(USER_ID, request))
                        .isInstanceOf(PaymentException.class)
                        .hasFieldOrPropertyWithValue("errorCode", "SIGNATURE_INVALID");
            }
        }

        @Test
        @DisplayName("Failure: Amount mismatch (<= 0)")
        void verify_AmountInvalid() {
            payment.setAmount(0);
            when(paymentRepository.findByRazorpayOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
            
            try (MockedStatic<Utils> mockedUtils = mockStatic(Utils.class)) {
                mockedUtils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), anyString())).thenReturn(true);
                
                assertThatThrownBy(() -> paymentService.verifyPayment(USER_ID, request))
                        .isInstanceOf(PaymentException.class)
                        .hasFieldOrPropertyWithValue("errorCode", "AMOUNT_MISMATCH");
                
                assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            }
        }
    }

    @Nested
    @DisplayName("Query methods tests")
    class QueryTests {

        @Test
        @DisplayName("getPaymentByOrderId: Success")
        void getByOrderId_Success() {
            Payment payment = Payment.builder()
                    .userId(USER_ID)
                    .razorpayOrderId(ORDER_ID)
                    .status(PaymentStatus.SUCCESS)
                    .type(PaymentType.SESSION_BOOKING)
                    .amount(50000)
                    .build();
            when(paymentRepository.findByRazorpayOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

            PaymentResponse response = paymentService.getPaymentByOrderId(USER_ID, ORDER_ID);

            assertThat(response.razorpayOrderId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("getPaymentByOrderId: Unauthorized")
        void getByOrderId_Unauthorized() {
            Payment payment = Payment.builder().userId(2L).razorpayOrderId(ORDER_ID).build();
            when(paymentRepository.findByRazorpayOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.getPaymentByOrderId(USER_ID, ORDER_ID))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "UNAUTHORIZED_ACCESS");
        }

        @Test
        @DisplayName("hasSuccessfulPayment: True path")
        void hasSuccessful_True() {
            when(paymentRepository.findByUserIdAndTypeAndStatus(USER_ID, PaymentType.SESSION_BOOKING, PaymentStatus.SUCCESS))
                    .thenReturn(List.of(new Payment()));

            boolean result = paymentService.hasSuccessfulPayment(USER_ID, PaymentType.SESSION_BOOKING);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("hasSuccessfulPayment: False path")
        void hasSuccessful_False() {
            when(paymentRepository.findByUserIdAndTypeAndStatus(USER_ID, PaymentType.SESSION_BOOKING, PaymentStatus.SUCCESS))
                    .thenReturn(Collections.emptyList());

            boolean result = paymentService.hasSuccessfulPayment(USER_ID, PaymentType.SESSION_BOOKING);

            assertThat(result).isFalse();
        }
    }
}
