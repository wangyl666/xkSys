package com.school.edu.repository;

import com.school.edu.entity.AttendanceAppeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttendanceAppealRepository extends JpaRepository<AttendanceAppeal, Long> {

    @Query("SELECT a FROM AttendanceAppeal a JOIN FETCH a.student s JOIN FETCH a.attendance att JOIN FETCH att.course c WHERE a.student.id = :studentId ORDER BY a.submittedAt DESC")
    List<AttendanceAppeal> findByStudentIdWithDetails(Long studentId);

    @Query("SELECT a FROM AttendanceAppeal a JOIN FETCH a.student s JOIN FETCH a.attendance att JOIN FETCH att.course c WHERE a.teacher.id = :teacherId ORDER BY a.submittedAt DESC")
    List<AttendanceAppeal> findByTeacherIdWithDetails(Long teacherId);

    @Query("SELECT a FROM AttendanceAppeal a JOIN FETCH a.student s JOIN FETCH a.attendance att JOIN FETCH att.course c WHERE a.teacher.id = :teacherId AND a.status = :status ORDER BY a.submittedAt DESC")
    List<AttendanceAppeal> findByTeacherIdAndStatusWithDetails(Long teacherId, AttendanceAppeal.AppealStatus status);

    @Query("SELECT a FROM AttendanceAppeal a JOIN FETCH a.student s JOIN FETCH a.attendance att JOIN FETCH att.course c WHERE a.status = :status ORDER BY a.submittedAt DESC")
    List<AttendanceAppeal> findByStatusWithDetails(AttendanceAppeal.AppealStatus status);

    boolean existsByAttendanceIdAndStatus(Long attendanceId, AttendanceAppeal.AppealStatus status);
}
