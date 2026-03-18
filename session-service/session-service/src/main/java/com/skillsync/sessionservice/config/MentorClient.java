//package com.skillsync.sessionservice.config;
//
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//
///**
// * Feign client to validate that a mentor exists before booking a session.
// * Falls back to MentorClientFallback on circuit breaker open or timeout.
// */
//@FeignClient(name = "mentor-service", fallback = MentorClientFallback.class)
//public interface MentorClient {
//
//    @GetMapping("/mentors/{id}/exists")
//    Boolean mentorExists(@PathVariable("id") Long mentorId);
//}
