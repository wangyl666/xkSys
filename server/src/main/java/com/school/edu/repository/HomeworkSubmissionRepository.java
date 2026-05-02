package com.school.edu.repository;

import com.school.edu.entity.HomeworkSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Long> {

    @Query("SELECT s FROM HomeworkSubmission s JOIN FETCH s.homework h JOIN FETCH s.student st WHERE s.homework.id = :homeworkId")
    List<HomeworkSubmission> findByHomeworkIdWithDetails(Long homeworkId);

    @Query("SELECT s FROM HomeworkSubmission s JOIN FETCH s.homework h JOIN FETCH s.student st WHERE s.student.id = :studentId")
    List<HomeworkSubmission> findByStudentIdWithDetails(Long studentId);

    @Query("SELECT s FROM HomeworkSubmission s JOIN FETCH s.homework h JOIN FETCH s.student st WHERE s.homework.id = :homeworkId AND s.student.id = :studentId")
    Optional<HomeworkSubmission> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);

    @Query("SELECT COUNT(s) FROM HomeworkSubmission s WHERE s.homework.id = :homeworkId AND s.status = 'SUBMITTED'")
    Long countSubmittedByHomeworkId(Long homeworkId);

    @Query("SELECT COUNT(s) FROM HomeworkSubmission s WHERE s.homework.id = :homeworkId AND s.status = 'GRADED'")
    Long countGradedByHomeworkId(Long homeworkId);

    @Query("SELECT s FROM HomeworkSubmission s JOIN FETCH s.homework h JOIN FETCH s.student st WHERE s.id = :id")
    Optional<HomeworkSubmission> findByIdWithDetails(Long id);

    @Query("SELECT s FROM HomeworkSubmission s JOIN FETCH s.homework h JOIN FETCH s.student st WHERE s.student.id = :studentId AND s.homework.id IN :homeworkIds")
    List<HomeworkSubmission> findByStudentIdAndHomeworkIds(Long studentId, List<Long> homeworkIds);
}
