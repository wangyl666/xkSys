package com.school.edu.service;

import com.school.edu.dto.NotificationDTO;
import com.school.edu.entity.Notification;
import com.school.edu.entity.User;
import com.school.edu.repository.NotificationRepository;
import com.school.edu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public NotificationDTO createNotification(Long userId, Notification.NotificationType type, String message, Long relatedId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRelatedId(relatedId);
        notification.setStatus(Notification.NotificationStatus.UNREAD);

        Notification saved = notificationRepository.save(notification);
        return toNotificationDTO(saved);
    }

    public List<NotificationDTO> getUserNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return notificationRepository.findByUserIdWithDetails(user.getId())
                .stream()
                .map(this::toNotificationDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getUserUnreadNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return notificationRepository.findUnreadByUserIdWithDetails(user.getId())
                .stream()
                .map(this::toNotificationDTO)
                .collect(Collectors.toList());
    }

    public Long getUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return notificationRepository.countUnreadByUserId(user.getId());
    }

    @Transactional
    public NotificationDTO markAsRead(Long id, String username) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("通知不存在"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("无权操作此通知");
        }

        notification.setStatus(Notification.NotificationStatus.READ);
        notification.setReadAt(java.time.LocalDateTime.now());

        return toNotificationDTO(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<Notification> unreadNotifications = notificationRepository
                .findUnreadByUserIdWithDetails(user.getId());

        for (Notification notification : unreadNotifications) {
            notification.setStatus(Notification.NotificationStatus.READ);
            notification.setReadAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    private NotificationDTO toNotificationDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUser().getId());
        dto.setUserName(notification.getUser().getName());
        dto.setType(notification.getType());
        dto.setMessage(notification.getMessage());
        dto.setRelatedId(notification.getRelatedId());
        dto.setStatus(notification.getStatus());
        dto.setSentAt(notification.getSentAt());
        dto.setReadAt(notification.getReadAt());
        return dto;
    }
}
