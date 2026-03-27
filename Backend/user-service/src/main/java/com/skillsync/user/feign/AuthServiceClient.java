package com.skillsync.user.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    @PutMapping("/api/auth/users/{id}/role")
    void updateUserRole(@PathVariable Long id, @RequestParam String role);
}
