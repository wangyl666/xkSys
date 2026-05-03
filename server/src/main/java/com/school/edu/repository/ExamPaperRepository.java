package com.school.edu.repository;

import com.school.edu.entity.ExamPaper;
import com.school.edu.entity.User;
import com.school.edu.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {
    List<ExamPaper> findByTeacher(User teacher);
    List<ExamPaper> findByTeacherId(Long teacherId);
    List<ExamPaper> findByCourse(Course course);
    List<ExamPaper> findByCourseId(Long courseId);
    List<ExamPaper> findByTeacherIdAndCourseId(Long teacherId, Long courseId);
    List<ExamPaper> findByStatus(ExamPaper.ExamPaperStatus status);
    List<ExamPaper> findByTeacherIdAndStatus(Long teacherId, ExamPaper.ExamPaperStatus status);
}
