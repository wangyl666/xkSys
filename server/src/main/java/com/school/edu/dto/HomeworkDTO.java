package com.school.edu.dto;

import lombok.Data;
import com.school.edu.entity.Homework;
import java.time.LocalDateTime;

@Data
public class HomeworkDTO {
    private Long id;
    private Long courseId;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Boolean enableReminder;
    private Integer reminderMinutes;
    private Homework.HomeworkStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer totalStudents;
    private Integer submittedCount;
    private Integer gradedCount;
    private HomeworkSubmissionDTO mySubmission;
}
