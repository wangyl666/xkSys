package com.school.edu.repository;

import com.school.edu.entity.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    @Query("SELECT h FROM Homework h JOIN FETCH h.course c JOIN FETCH h.teacher t WHERE h.teacher.id = :teacherId")
    List<Homework> findByTeacherIdWithDetails(Long teacherId);

    @Query("SELECT h FROM Homework h JOIN FETCH h.course c JOIN FETCH h.teacher t WHERE h.course.id = :courseId")
    List<Homework> findByCourseIdWithDetails(Long courseId);

    @Query("SELECT h FROM Homework h JOIN FETCH h.course c JOIN FETCH h.teacher t WHERE h.id = :id")
    java.util.Optional<Homework> findByIdWithDetails(Long id);

    @Query("SELECT h FROM Homework h JOIN FETCH h.course c WHERE c.id IN :courseIds AND h.status = 'PUBLISHED'")
    List<Homework> findPublishedByCourseIds(List<Long> courseIds);

    @Query("SELECT h FROM Homework h JOIN FETCH h.course c WHERE h.status = 'PUBLISHED' AND h.enableReminder = true")
    List<Homework> findPublishedWithReminderEnabled();

    @Query("SELECT h FROM Homework h JOIN FETCH h.course c WHERE h.status = 'PUBLISHED' AND h.deadline > :now AND h.enableReminder = true")
    List<Homework> findUpcomingWithReminderEnabled(LocalDateTime now);
}
