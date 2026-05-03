package com.school.edu.dto;

import lombok.Data;
import com.school.edu.entity.Question;

@Data
public class ExamPaperQuestionDTO {
    private Long id;
    private Long examPaperQuestionId;
    private Long examPaperId;
    private Long questionId;
    private Integer sortOrder;
    private Double score;

    private Question.QuestionType type;
    private String typeName;
    private String content;
    private String options;
    private String answer;
    private String explanation;
    private String difficulty;
}
