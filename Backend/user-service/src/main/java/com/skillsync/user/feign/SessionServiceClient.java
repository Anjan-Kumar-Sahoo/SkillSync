package com.skillsync.user.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "session-service")
public interface SessionServiceClient {
    @GetMapping("/api/sessions/count")
    Long getSessionCount();
}
