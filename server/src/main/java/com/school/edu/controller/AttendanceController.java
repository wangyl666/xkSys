package com.school.edu.controller;

import com.school.edu.dto.AttendanceDTO;
import com.school.edu.entity.Attendance;
import com.school.edu.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendances")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping
    public ResponseEntity<AttendanceDTO> createAttendance(
            @RequestBody AttendanceDTO attendanceDTO,
            @AuthenticationPrincipal String username) {
        AttendanceDTO created = attendanceService.createAttendance(attendanceDTO, username);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<AttendanceDTO>> batchCreateAttendance(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal String username) {
        
        Long courseId = Long.valueOf(request.get("courseId").toString());
        LocalDate attendanceDate = LocalDate.parse(request.get("attendanceDate").toString());
        @SuppressWarnings("unchecked")
        List<Long> presentStudentIds = (List<Long>) request.get("presentStudentIds");
        @SuppressWarnings("unchecked")
        List<Long> absentStudentIds = (List<Long>) request.get("absentStudentIds");
        @SuppressWarnings("unchecked")
        List<Long> lateStudentIds = (List<Long>) request.get("lateStudentIds");

        List<AttendanceDTO> attendances = attendanceService.batchCreateAttendance(
                courseId, attendanceDate, presentStudentIds, absentStudentIds, lateStudentIds, username);
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/teacher")
    public ResponseEntity<List<AttendanceDTO>> getTeacherAttendances(
            @AuthenticationPrincipal String username) {
        List<AttendanceDTO> attendances = attendanceService.getTeacherAttendances(username);
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/student")
    public ResponseEntity<List<AttendanceDTO>> getStudentAttendances(
            @AuthenticationPrincipal String username) {
        List<AttendanceDTO> attendances = attendanceService.getStudentAttendances(username);
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<AttendanceDTO>> getCourseAttendances(
            @PathVariable Long courseId) {
        List<AttendanceDTO> attendances = attendanceService.getCourseAttendances(courseId);
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/course/{courseId}/date/{date}")
    public ResponseEntity<List<AttendanceDTO>> getCourseAttendancesByDate(
            @PathVariable Long courseId,
            @PathVariable String date) {
        LocalDate attendanceDate = LocalDate.parse(date);
        List<AttendanceDTO> attendances = attendanceService.getCourseAttendancesByDate(courseId, attendanceDate);
        return ResponseEntity.ok(attendances);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AttendanceDTO> updateAttendanceStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Attendance.AttendanceStatus status = Attendance.AttendanceStatus.valueOf(request.get("status").toString());
        String remark = request.get("remark") != null ? request.get("remark").toString() : null;
        AttendanceDTO updated = attendanceService.updateAttendanceStatus(id, status, remark);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics/{courseId}")
    public ResponseEntity<List<AttendanceDTO>> getAbsentStatistics(
            @PathVariable Long courseId) {
        List<AttendanceDTO> statistics = attendanceService.getAbsentStatisticsByCourse(courseId);
        return ResponseEntity.ok(statistics);
    }
}
