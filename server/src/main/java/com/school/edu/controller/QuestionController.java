package com.school.edu.controller;

import com.school.edu.dto.QuestionDTO;
import com.school.edu.entity.User;
import com.school.edu.repository.UserRepository;
import com.school.edu.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<QuestionDTO>> getTeacherQuestions(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) Long courseId) {
        
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        List<QuestionDTO> questions;
        if (courseId != null) {
            questions = questionService.getQuestionsByCourse(currentUser.getId(), courseId);
        } else {
            questions = questionService.getTeacherQuestions(currentUser.getId());
        }
        
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/public")
    public ResponseEntity<List<QuestionDTO>> getPublicQuestions(
            @AuthenticationPrincipal String username) {
        
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        List<QuestionDTO> questions = questionService.getPublicQuestions(currentUser.getId());
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDTO> getQuestionById(@PathVariable Long id) {
        QuestionDTO question = questionService.getQuestionById(id);
        return ResponseEntity.ok(question);
    }

    @PostMapping
    public ResponseEntity<QuestionDTO> createQuestion(
            @Valid @RequestBody QuestionDTO questionDTO,
            @AuthenticationPrincipal String username) {
        QuestionDTO createdQuestion = questionService.createQuestion(questionDTO, username);
        return ResponseEntity.ok(createdQuestion);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<QuestionDTO>> batchCreateQuestions(
            @Valid @RequestBody List<QuestionDTO> questionDTOs,
            @AuthenticationPrincipal String username) {
        List<QuestionDTO> createdQuestions = questionService.batchCreateQuestions(questionDTOs, username);
        return ResponseEntity.ok(createdQuestions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionDTO> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionDTO questionDTO,
            @AuthenticationPrincipal String username) {
        QuestionDTO updatedQuestion = questionService.updateQuestion(id, questionDTO, username);
        return ResponseEntity.ok(updatedQuestion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteQuestion(
            @PathVariable Long id,
            @AuthenticationPrincipal String username) {
        questionService.deleteQuestion(id, username);
        Map<String, String> result = new HashMap<>();
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/types")
    public ResponseEntity<List<Map<String, Object>>> getQuestionTypes() {
        List<Map<String, Object>> types = java.util.Arrays.stream(
                com.school.edu.entity.Question.QuestionType.values())
                .map(type -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("value", type.name());
                    map.put("label", type.getDescription());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(types);
    }
}
