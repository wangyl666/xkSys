package com.school.edu.dto;

import lombok.Data;
import com.school.edu.entity.Course;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CourseDTO {
    private Long id;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private String description;

    @NotNull(message = "最大学生数不能为空")
    private Integer maxStudents;

    private Integer enrolledStudents;

    @NotNull(message = "学分不能为空")
    private Integer credits;

    private Course.DayOfWeek dayOfWeek;

    @NotNull(message = "开始节次不能为空")
    private Integer startSection;

    @NotNull(message = "结束节次不能为空")
    private Integer endSection;

    private String location;

    private String semester;

    private Course.CourseStatus status;

    private Long teacherId;
    private String teacherName;
}