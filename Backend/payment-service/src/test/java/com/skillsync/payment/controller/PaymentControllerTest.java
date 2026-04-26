package com.skillsync.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.payment.dto.*;
import com.skillsync.payment.enums.PaymentType;
import com.skillsync.payment.enums.ReferenceType;
import com.skillsync.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PaymentService paymentService;

    @Test
    @DisplayName("POST /create-order — creates Razorpay order (201)")
    void createOrder_shouldReturn201() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                PaymentType.SESSION_BOOKING, 1L, ReferenceType.SESSION_BOOKING, 1200);
        CreateOrderResponse response = new CreateOrderResponse(
                "order_123", 1200, "INR", "created", "rzp_key_123");
        when(paymentService.createOrder(eq(100L), any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/create-order")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order_123"));
    }

    @Test
    @DisplayName("POST /verify — verifies payment")
    void verifyPayment_shouldReturn200() throws Exception {
        VerifyPaymentRequest request = new VerifyPaymentRequest(
                "order_123", "pay_123", "sig_123");
        PaymentResponse response = new PaymentResponse(
                1L, 100L, "SESSION_BOOKING", 1200, "order_123", "pay_123",
                "CAPTURED", 1L, "SESSION_BOOKING", null, LocalDateTime.now(), LocalDateTime.now());
        when(paymentService.verifyPayment(eq(100L), any(VerifyPaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/verify")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CAPTURED"));
    }

    @Test
    @DisplayName("GET /my-payments — returns user's payments")
    void getMyPayments_shouldReturnList() throws Exception {
        when(paymentService.getUserPayments(100L)).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/my-payments").header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /order/{orderId} — returns payment by order ID")
    void getPaymentByOrderId_shouldReturnPayment() throws Exception {
        PaymentResponse response = new PaymentResponse(
                1L, 100L, "SESSION_BOOKING", 1200, "order_123", "pay_123",
                "CAPTURED", 1L, "SESSION_BOOKING", null, LocalDateTime.now(), LocalDateTime.now());
        when(paymentService.getPaymentByOrderId(100L, "order_123")).thenReturn(response);

        mockMvc.perform(get("/api/payments/order/order_123").header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_123"));
    }

    @Test
    @DisplayName("GET /check — checks payment status")
    void checkPaymentStatus_shouldReturnBoolean() throws Exception {
        when(paymentService.hasSuccessfulPayment(100L, PaymentType.SESSION_BOOKING)).thenReturn(true);

        mockMvc.perform(get("/api/payments/check")
                        .header("X-User-Id", "100")
                        .param("type", "SESSION_BOOKING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}
