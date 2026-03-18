package com.skillsync.apigateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.skillsync.apigateway.RouteValidator;

import reactor.core.publisher.Mono;

@Component
public class JwtFilter implements GlobalFilter {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // ✅ Allow auth APIs
        if (!validator.isSecured.test(request)) {
            return chain.filter(exchange);
        }

        // 🔐 Check Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing Authorization Header");
        }

        String token = authHeader.substring(7);

        // 🔥 Validate token
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or Expired Token");
        }

        return chain.filter(exchange);
    }
}
