package com.skillsync.apigateway;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RouteValidator {

    private static final List<String> openApi = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/send-otp",
            "/auth/verify-otp"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openApi.stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}