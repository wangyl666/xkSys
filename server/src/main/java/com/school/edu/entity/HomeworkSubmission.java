package com.school.edu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "homework_submissions")
public class HomeworkSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private Homework homework;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String fileUrl;

    private LocalDateTime submittedAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isLate = false;

    private Double score;

    @Column(columnDefinition = "TEXT")
    private String teacherComment;

    private LocalDateTime gradedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.NOT_SUBMITTED;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SubmissionStatus {
        NOT_SUBMITTED, SUBMITTED, GRADED
    }
}
