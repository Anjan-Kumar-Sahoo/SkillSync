package com.skillsync.payment.controller;

import com.skillsync.payment.entity.FailedEvent;
import com.skillsync.payment.service.DlqReplayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Internal admin controller for DLQ replay and monitoring.
 * These endpoints should NOT be exposed through the public API Gateway.
 * Access should be restricted to admin users or service-to-service calls only.
 */
@RestController
@RequestMapping("/internal/dlq")
@RequiredArgsConstructor
@Slf4j
public class DlqReplayController {

    private final DlqReplayService dlqReplayService;

    /**
     * Replay a specific failed event by its eventId.
     * The event is re-published to its original exchange/routing key.
     * Consumer-side idempotency ensures safe re-processing.
     */
    @PostMapping("/replay/{eventId}")
    public ResponseEntity<Map<String, String>> replayEvent(@PathVariable String eventId) {
        log.info("[DLQ-ADMIN] Replay requested for eventId={}", eventId);
        return ResponseEntity.ok(dlqReplayService.replayEvent(eventId));
    }

    /**
     * Skip a failed event (mark as intentionally not replayed).
     */
    @PostMapping("/skip/{eventId}")
    public ResponseEntity<Map<String, String>> skipEvent(@PathVariable String eventId) {
        log.info("[DLQ-ADMIN] Skip requested for eventId={}", eventId);
        return ResponseEntity.ok(dlqReplayService.skipEvent(eventId));
    }

    /**
     * List all failed events pending admin review.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<FailedEvent>> getPendingReview() {
        return ResponseEntity.ok(dlqReplayService.getPendingReview());
    }
}
