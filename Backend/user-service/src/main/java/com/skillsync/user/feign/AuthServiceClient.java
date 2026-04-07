package com.skillsync.user.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    @PutMapping("/api/auth/internal/users/{id}/role")
    void updateUserRole(@PathVariable("id") Long id, @RequestParam("role") String role);

    @GetMapping("/api/auth/internal/users")
    Map<String, Object> getAllUsers(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "search", required = false) String search);

    @GetMapping("/api/auth/internal/users/count")
    Long getUserCount(@RequestParam(value = "role", required = false) String role);

    @DeleteMapping("/api/auth/internal/users/{id}")
    void deleteUser(@PathVariable("id") Long id);

    @GetMapping("/api/auth/internal/users/{id}")
    Map<String, Object> getUserById(@PathVariable("id") Long id);
}
