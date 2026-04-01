package com.skillsync.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            var headers = exchange.getResponse().getHeaders();
            headers.add("X-Frame-Options", "DENY");
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("X-XSS-Protection", "1; mode=block");
            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
        }));
    }

    @Override
    public int getOrder() {
        return -1; // Run late to ensure headers are added to the final response
    }
}
