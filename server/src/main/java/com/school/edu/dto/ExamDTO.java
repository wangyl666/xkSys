package com.school.edu.dto;

import com.school.edu.entity.Exam;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamDTO {

    private Long id;

    private Long examPaperId;
    private String examPaperTitle;

    private Long courseId;
    private String courseName;

    private Long teacherId;
    private String teacherName;

    @NotBlank(message = "考试标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "总分不能为空")
    private Double totalScore;

    @NotNull(message = "考试时长不能为空")
    private Integer duration;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Exam.ExamStatus status;
    private String statusName;

    private Boolean scoresPublished;

    private LocalDateTime publishedAt;
    private String createdAt;
    private String updatedAt;

    private Integer submittedCount;
    private Integer totalStudents;
    private Integer gradedCount;

    private List<ExamPaperQuestionDTO> questions;

    private ExamSubmissionDTO mySubmission;
}
