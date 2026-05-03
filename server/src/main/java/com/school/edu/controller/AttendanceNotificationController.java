package com.school.edu.controller;

import com.school.edu.dto.AttendanceNotificationDTO;
import com.school.edu.service.AttendanceNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/attendance-notifications")
public class AttendanceNotificationController {

    @Autowired
    private AttendanceNotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<AttendanceNotificationDTO>> getStudentNotifications(
            @AuthenticationPrincipal String username) {
        List<AttendanceNotificationDTO> notifications = notificationService.getStudentNotifications(username);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<AttendanceNotificationDTO>> getStudentUnreadNotifications(
            @AuthenticationPrincipal String username) {
        List<AttendanceNotificationDTO> notifications = notificationService.getStudentUnreadNotifications(username);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal String username) {
        Long count = notificationService.getUnreadCount(username);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<AttendanceNotificationDTO> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        AttendanceNotificationDTO notification = notificationService.markAsRead(id, username);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal String username) {
        notificationService.markAllAsRead(username);
        return ResponseEntity.ok().build();
    }
}
