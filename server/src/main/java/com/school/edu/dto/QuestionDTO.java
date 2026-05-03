package com.school.edu.dto;

import lombok.Data;
import com.school.edu.entity.Question;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class QuestionDTO {
    private Long id;

    private Long courseId;
    private String courseName;

    private Long teacherId;
    private String teacherName;

    @NotNull(message = "题目类型不能为空")
    private Question.QuestionType type;

    private String typeName;

    @NotBlank(message = "题目内容不能为空")
    private String content;

    private String options;

    @NotBlank(message = "答案不能为空")
    private String answer;

    @NotNull(message = "分值不能为空")
    private Double score;

    private String explanation;

    private String tag;

    private String difficulty;

    private Boolean isPublic;

    private String createdAt;
    private String updatedAt;
}
