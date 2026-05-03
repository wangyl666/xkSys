package com.school.edu.controller;

import com.school.edu.dto.AttendanceAppealDTO;
import com.school.edu.service.AttendanceAppealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance-appeals")
public class AttendanceAppealController {

    @Autowired
    private AttendanceAppealService appealService;

    @PostMapping
    public ResponseEntity<AttendanceAppealDTO> createAppeal(
            @RequestBody AttendanceAppealDTO appealDTO,
            @AuthenticationPrincipal String username) {
        AttendanceAppealDTO created = appealService.createAppeal(appealDTO, username);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/student")
    public ResponseEntity<List<AttendanceAppealDTO>> getStudentAppeals(
            @AuthenticationPrincipal String username) {
        List<AttendanceAppealDTO> appeals = appealService.getStudentAppeals(username);
        return ResponseEntity.ok(appeals);
    }

    @GetMapping("/teacher")
    public ResponseEntity<List<AttendanceAppealDTO>> getTeacherAppeals(
            @AuthenticationPrincipal String username) {
        List<AttendanceAppealDTO> appeals = appealService.getTeacherAppeals(username);
        return ResponseEntity.ok(appeals);
    }

    @GetMapping("/teacher/pending")
    public ResponseEntity<List<AttendanceAppealDTO>> getPendingAppeals(
            @AuthenticationPrincipal String username) {
        List<AttendanceAppealDTO> appeals = appealService.getPendingAppeals(username);
        return ResponseEntity.ok(appeals);
    }

    @PutMapping("/{id}/review")
    public ResponseEntity<AttendanceAppealDTO> reviewAppeal(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal String username) {
        boolean approved = Boolean.parseBoolean(request.get("approved").toString());
        String teacherComment = request.get("teacherComment") != null ? request.get("teacherComment").toString() : null;
        AttendanceAppealDTO reviewed = appealService.reviewAppeal(id, approved, teacherComment, username);
        return ResponseEntity.ok(reviewed);
    }
}
