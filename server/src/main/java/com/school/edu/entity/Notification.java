package com.school.edu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "related_id")
    private Long relatedId;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.UNREAD;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }

    public enum NotificationType {
        ATTENDANCE_ABSENT,
        HOMEWORK_REMINDER,
        HOMEWORK_GRADED,
        SYSTEM
    }

    public enum NotificationStatus {
        UNREAD, READ
    }
}
