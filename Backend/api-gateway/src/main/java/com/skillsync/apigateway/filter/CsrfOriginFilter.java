package com.skillsync.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Component
public class CsrfOriginFilter implements GlobalFilter, Ordered {

    @Value("${app.cors.allowed-origins:https://skillsync.mraks.dev,https://skillsync.udayasri.dev}")
    private String allowedOrigins;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();
        
        // Never block CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }
        
        // Only apply to mutating methods
        if (List.of("POST", "PUT", "DELETE", "PATCH").contains(method)) {
            String origin = exchange.getRequest().getHeaders().getFirst("Origin");
            Set<String> allowed = Set.of(allowedOrigins.split(","));
            
            // If origin is present, it must be in the allowed list
            if (origin != null && !allowed.contains(origin.trim())) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -3; // Before rate limiting
    }
}
