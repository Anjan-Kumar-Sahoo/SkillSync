package com.skillsync.payment.dto;

public record CreateOrderResponse(
        String orderId,
        int amount,
        String currency,
        String status,
        String keyId
) {}
