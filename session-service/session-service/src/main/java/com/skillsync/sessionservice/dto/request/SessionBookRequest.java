//package com.skillsync.sessionservice.dto.request;
//
//import jakarta.validation.constraints.Future;
//import jakarta.validation.constraints.NotNull;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
///**
// * DTO for incoming session booking requests.
// * Uses Lombok to provide Getters, Setters, and Constructors.
// */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class SessionBookRequest {
//
//    @NotNull(message = "Mentor ID is required")
//    private Long mentorId;
//
//    @NotNull(message = "Learner ID is required")
//    private Long learnerId;
//
//    @NotNull(message = "Session date is required")
//    @Future(message = "Session date must be in the future")
//    private LocalDateTime sessionDate;
//
//    private String topic;
//}