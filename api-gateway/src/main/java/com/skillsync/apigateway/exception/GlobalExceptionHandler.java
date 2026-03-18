package com.skillsync.apigateway.exception;


import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        HttpStatus status = HttpStatus.UNAUTHORIZED;

        String error = "UNAUTHORIZED";
        String message = ex.getMessage();

        if (message == null || message.isBlank()) {
            message = "Unauthorized Access";
        }

        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        String timestamp = LocalDateTime.now().toString();
        String traceId = UUID.randomUUID().toString();

        // 🔥 JSON response (same as auth-service style)
        String response = "{"
                + "\"status\":\"ERROR\","
                + "\"message\":\"" + message + "\","
                + "\"data\":{"
                + "\"error\":\"" + error + "\","
                + "\"status\":" + status.value() + ","
                + "\"path\":\"" + path + "\","
                + "\"method\":\"" + method + "\","
                + "\"timestamp\":\"" + timestamp + "\","
                + "\"traceId\":\"" + traceId + "\""
                + "}"
                + "}";

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(response.getBytes());

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}