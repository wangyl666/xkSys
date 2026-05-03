package com.school.edu.dto;

import com.school.edu.entity.Question;
import lombok.Data;
import com.school.edu.entity.ExamPaper;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ExamPaperDTO {
    private Long id;

    private Long courseId;
    private String courseName;

    private Long teacherId;
    private String teacherName;

    @NotBlank(message = "试卷标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "总分不能为空")
    private Double totalScore;

    private Integer questionCount;

    @NotNull(message = "考试时长不能为空")
    private Integer duration;

    private ExamPaper.ExamPaperStatus status;
    private String statusName;

    private ExamPaper.CreationMethod creationMethod;
    private String creationMethodName;

    private String createdAt;
    private String updatedAt;

    private List<ExamPaperQuestionDTO> questions;

    private AutoGenerateConfig autoGenerateConfig;

    @Data
    public static class AutoGenerateConfig {
        private Long courseId;
        private Double totalScore;
        private Integer duration;
        private List<QuestionTypeConfig> typeConfigs;
    }

    @Data
    public static class QuestionTypeConfig {
        private Question.QuestionType type;
        private Integer count;
        private Double scorePerQuestion;
        private String difficulty;
    }
}
