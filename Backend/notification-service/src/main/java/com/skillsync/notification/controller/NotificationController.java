package com.skillsync.notification.controller;

import com.skillsync.notification.dto.NotificationResponse;
import com.skillsync.notification.service.command.NotificationCommandService;
import com.skillsync.notification.service.query.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;

    // ─── QUERIES ───

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestHeader("X-User-Id") Long userId, Pageable pageable) {
        return ResponseEntity.ok(notificationQueryService.getNotifications(userId, pageable));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(Map.of("count", notificationQueryService.getUnreadCount(userId)));
    }

    // ─── COMMANDS ───

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationCommandService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read/{id}")
    public ResponseEntity<Void> markAsReadPost(@PathVariable Long id) {
        notificationCommandService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        notificationCommandService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationCommandService.deleteNotification(id);
        return ResponseEntity.ok().build();
    }
}
