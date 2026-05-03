package com.school.edu.controller;

import com.school.edu.dto.ExamPaperDTO;
import com.school.edu.entity.User;
import com.school.edu.repository.UserRepository;
import com.school.edu.service.ExamPaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam-papers")
public class ExamPaperController {

    @Autowired
    private ExamPaperService examPaperService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ExamPaperDTO>> getTeacherExamPapers(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) Long courseId) {
        
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        List<ExamPaperDTO> examPapers;
        if (courseId != null) {
            examPapers = examPaperService.getExamPapersByCourse(currentUser.getId(), courseId);
        } else {
            examPapers = examPaperService.getTeacherExamPapers(currentUser.getId());
        }
        
        return ResponseEntity.ok(examPapers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamPaperDTO> getExamPaperById(@PathVariable Long id) {
        ExamPaperDTO examPaper = examPaperService.getExamPaperById(id);
        return ResponseEntity.ok(examPaper);
    }

    @PostMapping
    public ResponseEntity<ExamPaperDTO> createExamPaper(
            @Valid @RequestBody ExamPaperDTO examPaperDTO,
            @AuthenticationPrincipal String username) {
        ExamPaperDTO createdExamPaper = examPaperService.createExamPaper(examPaperDTO, username);
        return ResponseEntity.ok(createdExamPaper);
    }

    @PostMapping("/auto-generate")
    public ResponseEntity<ExamPaperDTO> autoGenerateExamPaper(
            @Valid @RequestBody ExamPaperDTO.AutoGenerateConfig config,
            @AuthenticationPrincipal String username) {
        ExamPaperDTO createdExamPaper = examPaperService.autoGenerateExamPaper(config, username);
        return ResponseEntity.ok(createdExamPaper);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExamPaperDTO> updateExamPaper(
            @PathVariable Long id,
            @Valid @RequestBody ExamPaperDTO examPaperDTO,
            @AuthenticationPrincipal String username) {
        ExamPaperDTO updatedExamPaper = examPaperService.updateExamPaper(id, examPaperDTO, username);
        return ResponseEntity.ok(updatedExamPaper);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteExamPaper(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        examPaperService.deleteExamPaper(id, username);
        Map<String, String> result = new HashMap<>();
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ExamPaperDTO> publishExamPaper(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        ExamPaperDTO publishedExamPaper = examPaperService.publishExamPaper(id, username);
        return ResponseEntity.ok(publishedExamPaper);
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, Object>>> getExamPaperStatuses() {
        List<Map<String, Object>> statuses = java.util.Arrays.stream(
                com.school.edu.entity.ExamPaper.ExamPaperStatus.values())
                .map(status -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("value", status.name());
                    map.put("label", status.getDescription());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/creation-methods")
    public ResponseEntity<List<Map<String, Object>>> getCreationMethods() {
        List<Map<String, Object>> methods = java.util.Arrays.stream(
                com.school.edu.entity.ExamPaper.CreationMethod.values())
                .map(method -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("value", method.name());
                    map.put("label", method.getDescription());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(methods);
    }
}
