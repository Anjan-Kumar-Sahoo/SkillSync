//package com.skillsync.sessionservice.config;
//
////import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
///**
// * Resilience4j fallback — invoked when mentor-service is down or circuit is open.
// * Returns true so that session booking is still attempted (fail-open strategy).
// * Adjust to false (fail-closed) if strict validation is required.
// */
////@Slf4j
//@Component
//public class MentorClientFallback implements MentorClient {
//
//    @Override
//    public Boolean mentorExists(Long mentorId) {
////        log.warn("mentor-service is unavailable. Falling back for mentorId={}", mentorId);
//        //return true;  // fail-open: allow booking even if mentor-service is down
//        //return false;// Fail-closed → reject booking
//        
//        throw new RuntimeException("Mentor Service Unavailable");
//    }
//}
