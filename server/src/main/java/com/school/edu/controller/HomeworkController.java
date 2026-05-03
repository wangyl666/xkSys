package com.school.edu.controller;

import com.school.edu.dto.HomeworkDTO;
import com.school.edu.dto.HomeworkSubmissionDTO;
import com.school.edu.service.HomeworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/homeworks")
public class HomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    @PostMapping
    public ResponseEntity<HomeworkDTO> createHomework(
            @RequestBody HomeworkDTO homeworkDTO,
            @AuthenticationPrincipal String username) {
        HomeworkDTO created = homeworkService.createHomework(homeworkDTO, username);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HomeworkDTO> updateHomework(
            @PathVariable Long id,
            @RequestBody HomeworkDTO homeworkDTO,
            @AuthenticationPrincipal String username) {
        HomeworkDTO updated = homeworkService.updateHomework(id, homeworkDTO, username);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<HomeworkDTO> publishHomework(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        HomeworkDTO published = homeworkService.publishHomework(id, username);
        return ResponseEntity.ok(published);
    }

    @GetMapping("/teacher")
    public ResponseEntity<List<HomeworkDTO>> getTeacherHomeworks(
            @AuthenticationPrincipal String username) {
        List<HomeworkDTO> homeworks = homeworkService.getTeacherHomeworks(username);
        return ResponseEntity.ok(homeworks);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<HomeworkDTO>> getCourseHomeworks(
            @PathVariable Long courseId) {
        List<HomeworkDTO> homeworks = homeworkService.getCourseHomeworks(courseId);
        return ResponseEntity.ok(homeworks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HomeworkDTO> getHomeworkById(@PathVariable Long id) {
        HomeworkDTO homework = homeworkService.getHomeworkById(id);
        return ResponseEntity.ok(homework);
    }

    @GetMapping("/{homeworkId}/submissions")
    public ResponseEntity<List<HomeworkSubmissionDTO>> getHomeworkSubmissions(
            @PathVariable Long homeworkId) {
        List<HomeworkSubmissionDTO> submissions = homeworkService.getHomeworkSubmissions(homeworkId);
        return ResponseEntity.ok(submissions);
    }

    @PostMapping("/{homeworkId}/submit")
    public ResponseEntity<HomeworkSubmissionDTO> submitHomework(
            @PathVariable Long homeworkId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal String username) {
        String content = request.get("content") != null ? request.get("content").toString() : null;
        String fileUrl = request.get("fileUrl") != null ? request.get("fileUrl").toString() : null;
        HomeworkSubmissionDTO submission = homeworkService.submitHomework(homeworkId, content, fileUrl, username);
        return ResponseEntity.ok(submission);
    }

    @PutMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<HomeworkSubmissionDTO> gradeHomework(
            @PathVariable Long submissionId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal String username) {
        Double score = request.get("score") != null ? Double.valueOf(request.get("score").toString()) : null;
        String teacherComment = request.get("teacherComment") != null ? request.get("teacherComment").toString() : null;
        HomeworkSubmissionDTO submission = homeworkService.gradeHomework(submissionId, score, teacherComment, username);
        return ResponseEntity.ok(submission);
    }

    @GetMapping("/student")
    public ResponseEntity<List<HomeworkDTO>> getStudentHomeworks(
            @AuthenticationPrincipal String username) {
        List<HomeworkDTO> homeworks = homeworkService.getStudentHomeworks(username);
        return ResponseEntity.ok(homeworks);
    }

    @GetMapping("/{homeworkId}/my-submission")
    public ResponseEntity<HomeworkSubmissionDTO> getMySubmission(
            @PathVariable Long homeworkId,
            @AuthenticationPrincipal String username) {
        HomeworkSubmissionDTO submission = homeworkService.getStudentSubmission(homeworkId, username);
        return ResponseEntity.ok(submission);
    }
}
