package com.school.edu.repository;

import com.school.edu.entity.ExamPaperQuestion;
import com.school.edu.entity.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamPaperQuestionRepository extends JpaRepository<ExamPaperQuestion, Long> {
    List<ExamPaperQuestion> findByExamPaper(ExamPaper examPaper);
    List<ExamPaperQuestion> findByExamPaperId(Long examPaperId);
    void deleteByExamPaperId(Long examPaperId);
}
