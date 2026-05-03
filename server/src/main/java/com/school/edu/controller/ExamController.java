package com.school.edu.controller;

import com.school.edu.dto.ExamAnswerDTO;
import com.school.edu.dto.ExamDTO;
import com.school.edu.dto.ExamSubmissionDTO;
import com.school.edu.entity.User;
import com.school.edu.repository.UserRepository;
import com.school.edu.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/teacher")
    public ResponseEntity<List<ExamDTO>> getTeacherExams(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) Long courseId) {

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        List<ExamDTO> exams = examService.getTeacherExams(currentUser.getId(), courseId);
        return ResponseEntity.ok(exams);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamDTO> getExamById(@PathVariable Long id) {
        ExamDTO exam = examService.getExamById(id);
        return ResponseEntity.ok(exam);
    }

    @GetMapping("/{id}/student")
    public ResponseEntity<ExamDTO> getExamForStudent(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        ExamDTO exam = examService.getExamForStudent(id, username);
        return ResponseEntity.ok(exam);
    }

    @PostMapping
    public ResponseEntity<ExamDTO> createExam(
            @Valid @RequestBody ExamDTO examDTO,
            @AuthenticationPrincipal String username) {
        ExamDTO createdExam = examService.createExam(examDTO, username);
        return ResponseEntity.ok(createdExam);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ExamDTO> publishExam(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        ExamDTO publishedExam = examService.publishExam(id, username);
        return ResponseEntity.ok(publishedExam);
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<List<ExamSubmissionDTO>> getExamSubmissions(@PathVariable Long id) {
        List<ExamSubmissionDTO> submissions = examService.getExamSubmissions(id);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/submissions/{submissionId}/grading")
    public ResponseEntity<ExamSubmissionDTO> getSubmissionForGrading(@PathVariable Long submissionId) {
        ExamSubmissionDTO submission = examService.getSubmissionForGrading(submissionId);
        return ResponseEntity.ok(submission);
    }

    @PutMapping("/answers/{answerId}/grade")
    public ResponseEntity<ExamAnswerDTO> gradeSubjectiveAnswer(
            @PathVariable Long answerId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal String username) {

        Double score = request.get("score") != null ? Double.valueOf(request.get("score").toString()) : null;
        String teacherComment = request.get("teacherComment") != null ? request.get("teacherComment").toString() : null;

        ExamAnswerDTO answer = examService.gradeSubjectiveAnswer(answerId, score, teacherComment, username);
        return ResponseEntity.ok(answer);
    }

    @PostMapping("/submissions/{submissionId}/complete-grading")
    public ResponseEntity<ExamSubmissionDTO> completeGrading(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal String username) {
        ExamSubmissionDTO submission = examService.completeGrading(submissionId, username);
        return ResponseEntity.ok(submission);
    }

    @PostMapping("/{id}/publish-scores")
    public ResponseEntity<ExamDTO> publishScores(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        ExamDTO exam = examService.publishScores(id, username);
        return ResponseEntity.ok(exam);
    }

    @GetMapping("/student")
    public ResponseEntity<List<ExamDTO>> getStudentExams(
            @AuthenticationPrincipal String username) {
        List<ExamDTO> exams = examService.getStudentExams(username);
        return ResponseEntity.ok(exams);
    }

    @PostMapping("/{examId}/start")
    public ResponseEntity<ExamSubmissionDTO> startExam(
            @PathVariable Long examId,
            @AuthenticationPrincipal String username) {
        ExamSubmissionDTO submission = examService.startExam(examId, username);
        return ResponseEntity.ok(submission);
    }

    @PutMapping("/submissions/{submissionId}/answers/{questionId}")
    public ResponseEntity<ExamAnswerDTO> saveAnswer(
            @PathVariable Long submissionId,
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal String username) {

        String studentAnswer = request.get("studentAnswer") != null ? request.get("studentAnswer").toString() : null;

        ExamAnswerDTO answer = examService.saveAnswer(submissionId, questionId, studentAnswer, username);
        return ResponseEntity.ok(answer);
    }

    @PostMapping("/submissions/{submissionId}/submit")
    public ResponseEntity<ExamSubmissionDTO> submitExam(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal String username) {
        ExamSubmissionDTO submission = examService.submitExam(submissionId, username);
        return ResponseEntity.ok(submission);
    }

    @GetMapping("/{examId}/my-submission")
    public ResponseEntity<ExamSubmissionDTO> getMySubmission(
            @PathVariable Long examId,
            @AuthenticationPrincipal String username) {
        ExamSubmissionDTO submission = examService.getMySubmission(examId, username);
        return ResponseEntity.ok(submission);
    }

    @GetMapping("/{examId}/my-result")
    public ResponseEntity<ExamSubmissionDTO> getMyResult(
            @PathVariable Long examId,
            @AuthenticationPrincipal String username) {
        ExamSubmissionDTO result = examService.getMyResult(examId, username);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, Object>>> getExamStatuses() {
        List<Map<String, Object>> statuses = java.util.Arrays.stream(
                com.school.edu.entity.Exam.ExamStatus.values())
                .map(status -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("value", status.name());
                    map.put("label", status.getDescription());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(statuses);
    }
}
