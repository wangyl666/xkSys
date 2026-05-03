package com.school.edu.dto;

import lombok.Data;
import com.school.edu.entity.Notification;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private Long userId;
    private String userName;
    private Notification.NotificationType type;
    private String message;
    private Long relatedId;
    private Notification.NotificationStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
