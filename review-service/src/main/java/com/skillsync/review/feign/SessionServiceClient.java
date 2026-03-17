package com.skillsync.review.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@FeignClient(name = "session-service")
public interface SessionServiceClient {
    @GetMapping("/api/sessions/{id}")
    Map<String, Object> getSessionById(@PathVariable Long id);
}
