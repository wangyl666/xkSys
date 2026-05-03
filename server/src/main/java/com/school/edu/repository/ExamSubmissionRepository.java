package com.school.edu.repository;

import com.school.edu.entity.ExamSubmission;
import com.school.edu.entity.Exam;
import com.school.edu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSubmissionRepository extends JpaRepository<ExamSubmission, Long> {

    List<ExamSubmission> findByExam(Exam exam);

    List<ExamSubmission> findByExamId(Long examId);

    Optional<ExamSubmission> findByExamIdAndStudentId(Long examId, Long studentId);

    List<ExamSubmission> findByStudentId(Long studentId);

    List<ExamSubmission> findByStudentIdAndStatus(Long studentId, ExamSubmission.SubmissionStatus status);

    @Query("SELECT es FROM ExamSubmission es WHERE es.exam.id IN :examIds AND es.student.id = :studentId")
    List<ExamSubmission> findByExamIdsAndStudentId(@Param("examIds") List<Long> examIds, @Param("studentId") Long studentId);

    @Query("SELECT es FROM ExamSubmission es LEFT JOIN FETCH es.answers WHERE es.id = :id")
    Optional<ExamSubmission> findByIdWithAnswers(@Param("id") Long id);

    @Query("SELECT es FROM ExamSubmission es LEFT JOIN FETCH es.answers WHERE es.exam.id = :examId")
    List<ExamSubmission> findByExamIdWithAnswers(@Param("examId") Long examId);

    @Query("SELECT COUNT(es) FROM ExamSubmission es WHERE es.exam.id = :examId AND es.status = :status")
    Long countByExamIdAndStatus(@Param("examId") Long examId, @Param("status") ExamSubmission.SubmissionStatus status);
}
