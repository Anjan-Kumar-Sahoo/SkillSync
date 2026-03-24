package com.skillsync.user.dto;

public record CreateOrderResponse(
        String orderId,
        int amount,
        String currency,
        String status,
        String keyId
) {}
