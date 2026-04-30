package com.skillsync.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.payment.dto.CreateOrderResponse;
import com.skillsync.payment.dto.PaymentResponse;
import com.skillsync.payment.dto.VerifyPaymentRequest;
import com.skillsync.payment.enums.PaymentStatus;
import com.skillsync.payment.enums.PaymentType;
import com.skillsync.payment.enums.ReferenceType;
import com.skillsync.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private PaymentService paymentService;

    @Test
    @DisplayName("createOrder: Success path")
    void createOrder_Success() throws Exception {
        CreateOrderResponse response = new CreateOrderResponse(
                "order_123", 1000, "INR", "CREATED", "test_key");
        
        when(paymentService.createOrder(anyLong(), any())).thenReturn(response);

        String json = "{\"type\":\"SESSION_BOOKING\",\"referenceId\":101,\"referenceType\":\"SESSION_BOOKING\",\"amount\":10}";

        mockMvc.perform(post("/api/payments/create-order")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order_123"));
    }

    @Test
    @DisplayName("verifyPayment: Success path")
    void verifyPayment_Success() throws Exception {
        VerifyPaymentRequest request = new VerifyPaymentRequest("order_123", "pay_123", "sig_123");
        PaymentResponse response = new PaymentResponse(
                1L, 1L, PaymentType.SESSION_BOOKING.name(), 1000, 
                "order_123", "pay_123", PaymentStatus.VERIFIED.name(), 
                101L, ReferenceType.SESSION_BOOKING.name(), null, 
                LocalDateTime.now(), LocalDateTime.now());

        when(paymentService.verifyPayment(anyLong(), any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/verify")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(PaymentStatus.VERIFIED.name()));
    }

    @Test
    @DisplayName("getPaymentByOrderId: Success path")
    void getPayment_Success() throws Exception {
        PaymentResponse response = new PaymentResponse(
                1L, 1L, PaymentType.SESSION_BOOKING.name(), 1000, 
                "order_123", "pay_123", PaymentStatus.SUCCESS.name(), 
                101L, ReferenceType.SESSION_BOOKING.name(), null, 
                LocalDateTime.now(), LocalDateTime.now());
        
        when(paymentService.getPaymentByOrderId(anyLong(), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/payments/order/order_123")
                .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_123"));
    }
}
