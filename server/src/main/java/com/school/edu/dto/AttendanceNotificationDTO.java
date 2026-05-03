package com.school.edu.dto;

import lombok.Data;
import com.school.edu.entity.AttendanceNotification;
import java.time.LocalDateTime;

@Data
public class AttendanceNotificationDTO {
    private Long id;
    private Long attendanceId;
    private Long studentId;
    private String studentName;
    private String courseName;
    private String message;
    private AttendanceNotification.NotificationStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
