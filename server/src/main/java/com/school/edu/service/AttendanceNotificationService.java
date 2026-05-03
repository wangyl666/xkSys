package com.school.edu.service;

import com.school.edu.dto.AttendanceNotificationDTO;
import com.school.edu.entity.AttendanceNotification;
import com.school.edu.entity.User;
import com.school.edu.repository.AttendanceNotificationRepository;
import com.school.edu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceNotificationService {

    @Autowired
    private AttendanceNotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    public List<AttendanceNotificationDTO> getStudentNotifications(String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        return notificationRepository.findByStudentIdWithDetails(student.getId())
                .stream()
                .map(this::toNotificationDTO)
                .collect(Collectors.toList());
    }

    public List<AttendanceNotificationDTO> getStudentUnreadNotifications(String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        return notificationRepository.findByStudentIdAndStatusWithDetails(
                student.getId(), AttendanceNotification.NotificationStatus.UNREAD)
                .stream()
                .map(this::toNotificationDTO)
                .collect(Collectors.toList());
    }

    public Long getUnreadCount(String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        return notificationRepository.countUnreadByStudentId(student.getId());
    }

    @Transactional
    public AttendanceNotificationDTO markAsRead(Long id, String studentUsername) {
        AttendanceNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("通知不存在"));

        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        if (!notification.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("无权操作此通知");
        }

        notification.setStatus(AttendanceNotification.NotificationStatus.READ);
        notification.setReadAt(java.time.LocalDateTime.now());

        return toNotificationDTO(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        List<AttendanceNotification> unreadNotifications = notificationRepository
                .findByStudentIdAndStatusWithDetails(student.getId(), AttendanceNotification.NotificationStatus.UNREAD);

        for (AttendanceNotification notification : unreadNotifications) {
            notification.setStatus(AttendanceNotification.NotificationStatus.READ);
            notification.setReadAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    private AttendanceNotificationDTO toNotificationDTO(AttendanceNotification notification) {
        AttendanceNotificationDTO dto = new AttendanceNotificationDTO();
        dto.setId(notification.getId());
        dto.setAttendanceId(notification.getAttendance().getId());
        dto.setStudentId(notification.getStudent().getId());
        dto.setStudentName(notification.getStudent().getName());
        dto.setCourseName(notification.getAttendance().getCourse().getCourseName());
        dto.setMessage(notification.getMessage());
        dto.setStatus(notification.getStatus());
        dto.setSentAt(notification.getSentAt());
        dto.setReadAt(notification.getReadAt());
        return dto;
    }
}
