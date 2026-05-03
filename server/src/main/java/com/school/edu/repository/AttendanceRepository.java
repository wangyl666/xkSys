package com.school.edu.repository;

import com.school.edu.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH a.course c WHERE c.teacher.id = :teacherId")
    List<Attendance> findByTeacherIdWithDetails(Long teacherId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH a.course c WHERE a.student.id = :studentId")
    List<Attendance> findByStudentIdWithDetails(Long studentId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH a.course c WHERE a.course.id = :courseId")
    List<Attendance> findByCourseIdWithDetails(Long courseId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH a.course c WHERE a.course.id = :courseId AND a.attendanceDate = :date")
    List<Attendance> findByCourseIdAndDateWithDetails(Long courseId, LocalDate date);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH a.course c WHERE a.student.id = :studentId AND a.status = 'ABSENT'")
    List<Attendance> findAbsentByStudentIdWithDetails(Long studentId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH a.course c WHERE a.course.id = :courseId AND a.status = 'ABSENT'")
    List<Attendance> findAbsentByCourseIdWithDetails(Long courseId);

    boolean existsByCourseIdAndAttendanceDateAndStudentId(Long courseId, LocalDate attendanceDate, Long studentId);

    @Query("SELECT a FROM Attendance a WHERE a.course.id = :courseId AND a.attendanceDate = :attendanceDate AND a.student.id = :studentId")
    java.util.Optional<Attendance> findByCourseIdAndDateAndStudentId(Long courseId, LocalDate attendanceDate, Long studentId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.status = 'ABSENT'")
    Long countAbsentByStudentId(Long studentId);
}
