package com.school.edu.controller;

import com.school.edu.dto.CourseDTO;
import com.school.edu.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        List<CourseDTO> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/teacher")
    public ResponseEntity<List<CourseDTO>> getTeacherCourses(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) Long teacherId) {
        
        List<CourseDTO> courses;
        if (teacherId != null) {
            courses = courseService.getTeacherCourses(teacherId);
        } else {
            courses = courseService.getTeacherCourses(null);
        }
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDTO> getCourseById(@PathVariable Long id) {
        CourseDTO course = courseService.getCourseById(id);
        return ResponseEntity.ok(course);
    }

    @PostMapping
    public ResponseEntity<CourseDTO> createCourse(
            @Valid @RequestBody CourseDTO courseDTO,
            @AuthenticationPrincipal String username) {
        CourseDTO createdCourse = courseService.createCourse(courseDTO, username);
        return ResponseEntity.ok(createdCourse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseDTO> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDTO courseDTO,
            @AuthenticationPrincipal String username) {
        CourseDTO updatedCourse = courseService.updateCourse(id, courseDTO, username);
        return ResponseEntity.ok(updatedCourse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        courseService.deleteCourse(id, username);
        return ResponseEntity.noContent().build();
    }
}