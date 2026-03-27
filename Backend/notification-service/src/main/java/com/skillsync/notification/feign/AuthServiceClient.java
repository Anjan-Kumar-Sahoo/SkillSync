package com.skillsync.notification.feign;

import com.skillsync.notification.dto.UserSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/auth/internal/users/{id}")
    UserSummary getUserById(@PathVariable("id") Long id);
}
