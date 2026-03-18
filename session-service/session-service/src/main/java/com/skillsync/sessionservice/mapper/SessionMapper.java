//package com.skillsync.sessionservice.mapper;
//
//import com.skillsync.sessionservice.dto.request.SessionBookRequest;
//import com.skillsync.sessionservice.dto.response.SessionResponse;
//import com.skillsync.sessionservice.entity.Session;
//import com.skillsync.sessionservice.entity.SessionStatus;
//import org.springframework.stereotype.Component;
//
//@Component
//public class SessionMapper {
//
//    public Session toEntity(SessionBookRequest request) {
//        return Session.builder()
//                .mentorId(request.getMentorId())
//                .learnerId(request.getLearnerId())
//                .sessionDate(request.getSessionDate())
//                .topic(request.getTopic())
//                .status(SessionStatus.REQUESTED)
//                .build();
//    }
//
//    public SessionResponse toResponse(Session session) {
//        return new SessionResponse(
//                session.getId(),
//                session.getMentorId(),
//                session.getLearnerId(),
//                session.getSessionDate(),
//                session.getStatus(),
//                session.getTopic(),
//                session.getCreatedAt()
//        );
//    }
//}
