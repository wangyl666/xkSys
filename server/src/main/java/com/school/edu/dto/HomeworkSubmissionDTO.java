package com.school.edu.dto;

import lombok.Data;
import com.school.edu.entity.HomeworkSubmission;
import java.time.LocalDateTime;

@Data
public class HomeworkSubmissionDTO {
    private Long id;
    private Long homeworkId;
    private String homeworkTitle;
    private LocalDateTime homeworkDeadline;
    private Long studentId;
    private String studentName;
    private String studentUsername;
    private String content;
    private String fileUrl;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private Boolean isLate;
    private Double score;
    private String teacherComment;
    private LocalDateTime gradedAt;
    private HomeworkSubmission.SubmissionStatus status;
}
