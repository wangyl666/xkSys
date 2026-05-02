package com.school.edu.controller;

import com.school.edu.dto.CourseDTO;
import com.school.edu.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @GetMapping("/my-courses")
    public ResponseEntity<List<CourseDTO>> getEnrolledCourses(
            @AuthenticationPrincipal String username) {
        List<CourseDTO> courses = enrollmentService.getEnrolledCourses(username);
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/enroll/{courseId}")
    public ResponseEntity<Void> enrollCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal String username) {
        enrollmentService.enrollCourse(courseId, username);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/drop/{courseId}")
    public ResponseEntity<Void> dropCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal String username) {
        enrollmentService.dropCourse(courseId, username);
        return ResponseEntity.noContent().build();
    }
}