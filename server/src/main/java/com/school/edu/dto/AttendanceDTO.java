package com.school.edu.dto;

import lombok.Data;
import com.school.edu.entity.Attendance;
import java.time.LocalDate;

@Data
public class AttendanceDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentUsername;
    private Long courseId;
    private String courseName;
    private LocalDate attendanceDate;
    private String dayOfWeek;
    private Integer startSection;
    private Integer endSection;
    private Attendance.AttendanceStatus status;
    private Long createdById;
    private String createdByName;
    private String remark;
    private Boolean hasAppeal;
    private String appealStatus;
}
