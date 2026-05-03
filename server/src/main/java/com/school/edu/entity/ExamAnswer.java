package com.school.edu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exam_answers")
public class ExamAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_submission_id", nullable = false)
    private ExamSubmission examSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_paper_question_id", nullable = false)
    private ExamPaperQuestion examPaperQuestion;

    @Column(columnDefinition = "TEXT")
    private String studentAnswer;

    @Column(nullable = false)
    private Boolean isAnswered = false;

    private Double score;

    @Column(nullable = false)
    private Boolean isAutoGraded = false;

    private Boolean isCorrect;

    @Column(length = 500)
    private String teacherComment;

    private LocalDateTime answeredAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
