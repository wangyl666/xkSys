package com.school.edu.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "exam_paper_questions")
public class ExamPaperQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_paper_id", nullable = false)
    private ExamPaper examPaper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Double score;
}
