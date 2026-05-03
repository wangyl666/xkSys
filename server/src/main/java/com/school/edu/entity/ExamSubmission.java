package com.school.edu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "exam_submissions")
public class ExamSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.NOT_STARTED;

    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    private Double totalScore;

    private Double objectiveScore;

    private Double subjectiveScore;

    private LocalDateTime gradedAt;

    @Column(length = 1000)
    private String teacherComment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "examSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamAnswer> answers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SubmissionStatus {
        NOT_STARTED("未开始"),
        IN_PROGRESS("进行中"),
        SUBMITTED("已提交"),
        GRADED("已评分");

        private final String description;

        SubmissionStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
