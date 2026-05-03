package com.school.edu.dto;

import com.school.edu.entity.ExamSubmission;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamSubmissionDTO {

    private Long id;

    private Long examId;
    private String examTitle;

    private Long studentId;
    private String studentName;
    private String studentUsername;

    private ExamSubmission.SubmissionStatus status;
    private String statusName;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime gradedAt;

    private Double totalScore;
    private Double objectiveScore;
    private Double subjectiveScore;
    private Double maxScore;

    private String teacherComment;

    private List<ExamAnswerDTO> answers;
}
