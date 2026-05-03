package com.school.edu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "exam_papers")
public class ExamPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Double totalScore;

    @Column(nullable = false)
    private Integer questionCount;

    @Column(nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    private ExamPaperStatus status = ExamPaperStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    private CreationMethod creationMethod;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "examPaper", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamPaperQuestion> questions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ExamPaperStatus {
        DRAFT("草稿"),
        PUBLISHED("已发布"),
        ARCHIVED("已归档");

        private final String description;

        ExamPaperStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum CreationMethod {
        AUTO("自动组卷"),
        MANUAL("手动组卷");

        private final String description;

        CreationMethod(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
