package com.school.edu.repository;

import com.school.edu.entity.ExamAnswer;
import com.school.edu.entity.ExamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, Long> {

    List<ExamAnswer> findByExamSubmission(ExamSubmission examSubmission);

    List<ExamAnswer> findByExamSubmissionId(Long submissionId);

    Optional<ExamAnswer> findByExamSubmissionIdAndExamPaperQuestionId(Long submissionId, Long questionId);

    @Query("SELECT ea FROM ExamAnswer ea WHERE ea.examSubmission.id = :submissionId ORDER BY ea.examPaperQuestion.sortOrder ASC")
    List<ExamAnswer> findBySubmissionIdOrdered(@Param("submissionId") Long submissionId);

    @Query("SELECT COUNT(ea) FROM ExamAnswer ea WHERE ea.examSubmission.id = :submissionId AND ea.isAnswered = true")
    Long countAnsweredBySubmissionId(@Param("submissionId") Long submissionId);

    @Query("SELECT SUM(ea.score) FROM ExamAnswer ea WHERE ea.examSubmission.id = :submissionId")
    Double sumScoresBySubmissionId(@Param("submissionId") Long submissionId);
}
