package com.school.edu.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExamAnswerDTO {

    private Long id;

    private Long examSubmissionId;
    private Long examPaperQuestionId;

    private String studentAnswer;
    private Boolean isAnswered;

    private Double score;
    private Double maxScore;

    private Boolean isAutoGraded;
    private Boolean isCorrect;

    private String teacherComment;

    private LocalDateTime answeredAt;

    private Integer sortOrder;
    private String type;
    private String typeName;
    private String content;
    private String options;
    private String correctAnswer;
    private String explanation;
}
