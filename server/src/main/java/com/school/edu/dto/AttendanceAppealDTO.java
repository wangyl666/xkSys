package com.school.edu.dto;

import lombok.Data;
import com.school.edu.entity.AttendanceAppeal;
import java.time.LocalDateTime;

@Data
public class AttendanceAppealDTO {
    private Long id;
    private Long attendanceId;
    private Long studentId;
    private String studentName;
    private String studentUsername;
    private Long teacherId;
    private String teacherName;
    private String courseName;
    private String attendanceDate;
    private String dayOfWeek;
    private Integer startSection;
    private Integer endSection;
    private String reason;
    private String evidence;
    private AttendanceAppeal.AppealStatus status;
    private String teacherComment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
