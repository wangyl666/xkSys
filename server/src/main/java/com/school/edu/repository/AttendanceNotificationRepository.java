package com.school.edu.repository;

import com.school.edu.entity.AttendanceNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttendanceNotificationRepository extends JpaRepository<AttendanceNotification, Long> {

    @Query("SELECT n FROM AttendanceNotification n JOIN FETCH n.student s JOIN FETCH n.attendance a JOIN FETCH a.course c WHERE n.student.id = :studentId ORDER BY n.sentAt DESC")
    List<AttendanceNotification> findByStudentIdWithDetails(Long studentId);

    @Query("SELECT n FROM AttendanceNotification n JOIN FETCH n.student s JOIN FETCH n.attendance a JOIN FETCH a.course c WHERE n.student.id = :studentId AND n.status = :status ORDER BY n.sentAt DESC")
    List<AttendanceNotification> findByStudentIdAndStatusWithDetails(Long studentId, AttendanceNotification.NotificationStatus status);

    @Query("SELECT COUNT(n) FROM AttendanceNotification n WHERE n.student.id = :studentId AND n.status = 'UNREAD'")
    Long countUnreadByStudentId(Long studentId);
}
