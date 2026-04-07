package com.skillsync.session.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "user-service")
public interface MentorProfileClient {

    @GetMapping("/api/mentors/{id}")
    Map<String, Object> getMentorById(@PathVariable("id") Long id);
}
