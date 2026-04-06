package com.skillsync.user.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    @PutMapping("/api/auth/internal/users/{id}/role")
    void updateUserRole(@PathVariable("id") Long id, @RequestParam("role") String role);

    @GetMapping("/api/auth/internal/users")
    Map<String, Object> getAllUsers(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/api/auth/internal/users/count")
    Long getUserCount(@RequestParam(value = "role", required = false) String role);
}
