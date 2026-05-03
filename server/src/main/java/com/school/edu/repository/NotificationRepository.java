package com.school.edu.repository;

import com.school.edu.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n JOIN FETCH n.user u WHERE n.user.id = :userId ORDER BY n.sentAt DESC")
    List<Notification> findByUserIdWithDetails(Long userId);

    @Query("SELECT n FROM Notification n JOIN FETCH n.user u WHERE n.user.id = :userId AND n.status = 'UNREAD' ORDER BY n.sentAt DESC")
    List<Notification> findUnreadByUserIdWithDetails(Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.status = 'UNREAD'")
    Long countUnreadByUserId(Long userId);

    @Query("SELECT n FROM Notification n JOIN FETCH n.user u WHERE n.user.id = :userId AND n.type = :type ORDER BY n.sentAt DESC")
    List<Notification> findByUserIdAndTypeWithDetails(Long userId, Notification.NotificationType type);
}
