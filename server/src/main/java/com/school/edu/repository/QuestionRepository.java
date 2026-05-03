package com.school.edu.repository;

import com.school.edu.entity.Question;
import com.school.edu.entity.Course;
import com.school.edu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTeacher(User teacher);
    List<Question> findByTeacherId(Long teacherId);
    List<Question> findByCourse(Course course);
    List<Question> findByCourseId(Long courseId);
    List<Question> findByTeacherIdAndCourseId(Long teacherId, Long courseId);
    List<Question> findByType(Question.QuestionType type);
    List<Question> findByDifficulty(String difficulty);
    List<Question> findByIsPublicTrue();
    List<Question> findByTeacherIdOrIsPublicTrue(Long teacherId);
}
