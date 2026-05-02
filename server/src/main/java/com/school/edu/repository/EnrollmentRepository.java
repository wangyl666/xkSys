package com.school.edu.repository;

import com.school.edu.entity.Enrollment;
import com.school.edu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentAndStatus(User student, Enrollment.EnrollmentStatus status);
    List<Enrollment> findByCourseIdAndStatus(Long courseId, Enrollment.EnrollmentStatus status);
    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, Enrollment.EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course c JOIN FETCH c.teacher t WHERE e.student.id = :studentId AND e.status = 'ENROLLED'")
    List<Enrollment> findEnrolledCoursesWithDetails(Long studentId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student s WHERE e.course.id = :courseId AND e.status = 'ENROLLED'")
    List<Enrollment> findEnrolledStudentsWithDetails(Long courseId);
}