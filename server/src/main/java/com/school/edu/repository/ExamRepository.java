package com.school.edu.repository;

import com.school.edu.entity.Exam;
import com.school.edu.entity.User;
import com.school.edu.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByTeacher(User teacher);

    List<Exam> findByTeacherId(Long teacherId);

    List<Exam> findByCourse(Course course);

    List<Exam> findByCourseId(Long courseId);

    List<Exam> findByTeacherIdAndCourseId(Long teacherId, Long courseId);

    List<Exam> findByStatus(Exam.ExamStatus status);

    List<Exam> findByTeacherIdAndStatus(Long teacherId, Exam.ExamStatus status);

    List<Exam> findByCourseIdAndStatus(Long courseId, Exam.ExamStatus status);

    @Query("SELECT e FROM Exam e WHERE e.course.id IN :courseIds AND e.status = :status")
    List<Exam> findByCourseIdsAndStatus(@Param("courseIds") List<Long> courseIds, @Param("status") Exam.ExamStatus status);

    @Query("SELECT e FROM Exam e WHERE e.course.id IN :courseIds AND e.status IN :statuses")
    List<Exam> findByCourseIdsAndStatuses(@Param("courseIds") List<Long> courseIds, @Param("statuses") List<Exam.ExamStatus> statuses);
}
