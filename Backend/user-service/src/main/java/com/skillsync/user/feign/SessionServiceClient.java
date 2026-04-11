package com.skillsync.user.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "session-service")
public interface SessionServiceClient {
    @GetMapping("/api/sessions/count")
    Long getSessionCount();

    @GetMapping("/api/sessions/mentor/{mentorId}/metrics")
    Map<String, Object> getMentorMetrics(@PathVariable("mentorId") Long mentorId);
}
