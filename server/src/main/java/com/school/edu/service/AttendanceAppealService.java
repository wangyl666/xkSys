package com.school.edu.service;

import com.school.edu.dto.AttendanceAppealDTO;
import com.school.edu.entity.*;
import com.school.edu.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceAppealService {

    @Autowired
    private AttendanceAppealRepository appealRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceNotificationRepository notificationRepository;

    @Transactional
    public AttendanceAppealDTO createAppeal(AttendanceAppealDTO dto, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        if (student.getRole() != User.Role.STUDENT) {
            throw new RuntimeException("只有学生可以发起申诉");
        }

        Attendance attendance = attendanceRepository.findById(dto.getAttendanceId())
                .orElseThrow(() -> new RuntimeException("考勤记录不存在"));

        if (!attendance.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("只能对自己的考勤记录发起申诉");
        }

        if (appealRepository.existsByAttendanceIdAndStatus(
                attendance.getId(), AttendanceAppeal.AppealStatus.PENDING)) {
            throw new RuntimeException("该考勤记录已有待审核的申诉");
        }

        AttendanceAppeal appeal = new AttendanceAppeal();
        appeal.setAttendance(attendance);
        appeal.setStudent(student);
        appeal.setTeacher(attendance.getCourse().getTeacher());
        appeal.setReason(dto.getReason());
        appeal.setEvidence(dto.getEvidence());
        appeal.setStatus(AttendanceAppeal.AppealStatus.PENDING);

        AttendanceAppeal savedAppeal = appealRepository.save(appeal);
        return toAppealDTO(savedAppeal);
    }

    public List<AttendanceAppealDTO> getStudentAppeals(String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        return appealRepository.findByStudentIdWithDetails(student.getId())
                .stream()
                .map(this::toAppealDTO)
                .collect(Collectors.toList());
    }

    public List<AttendanceAppealDTO> getTeacherAppeals(String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        return appealRepository.findByTeacherIdWithDetails(teacher.getId())
                .stream()
                .map(this::toAppealDTO)
                .collect(Collectors.toList());
    }

    public List<AttendanceAppealDTO> getPendingAppeals(String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        return appealRepository.findByTeacherIdAndStatusWithDetails(
                teacher.getId(), AttendanceAppeal.AppealStatus.PENDING)
                .stream()
                .map(this::toAppealDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AttendanceAppealDTO reviewAppeal(Long appealId, boolean approved, 
            String teacherComment, String teacherUsername) {
        
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("只有教师可以审核申诉");
        }

        AttendanceAppeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new RuntimeException("申诉不存在"));

        if (!appeal.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能审核自己课程的申诉");
        }

        if (appeal.getStatus() != AttendanceAppeal.AppealStatus.PENDING) {
            throw new RuntimeException("该申诉已被审核");
        }

        appeal.setStatus(approved ? AttendanceAppeal.AppealStatus.APPROVED : AttendanceAppeal.AppealStatus.REJECTED);
        appeal.setTeacherComment(teacherComment);
        appeal.setReviewedAt(java.time.LocalDateTime.now());

        AttendanceAppeal savedAppeal = appealRepository.save(appeal);

        if (approved) {
            Attendance attendance = appeal.getAttendance();
            attendance.setStatus(Attendance.AttendanceStatus.EXCUSED);
            attendance.setRemark("申诉通过，已修改为事假");
            attendanceRepository.save(attendance);
        }

        return toAppealDTO(savedAppeal);
    }

    private AttendanceAppealDTO toAppealDTO(AttendanceAppeal appeal) {
        AttendanceAppealDTO dto = new AttendanceAppealDTO();
        dto.setId(appeal.getId());
        dto.setAttendanceId(appeal.getAttendance().getId());
        dto.setStudentId(appeal.getStudent().getId());
        dto.setStudentName(appeal.getStudent().getName());
        dto.setStudentUsername(appeal.getStudent().getUsername());
        if (appeal.getTeacher() != null) {
            dto.setTeacherId(appeal.getTeacher().getId());
            dto.setTeacherName(appeal.getTeacher().getName());
        }
        dto.setCourseName(appeal.getAttendance().getCourse().getCourseName());
        dto.setAttendanceDate(appeal.getAttendance().getAttendanceDate().toString());
        dto.setDayOfWeek(formatDayOfWeek(appeal.getAttendance().getCourse().getDayOfWeek()));
        dto.setStartSection(appeal.getAttendance().getStartSection());
        dto.setEndSection(appeal.getAttendance().getEndSection());
        dto.setReason(appeal.getReason());
        dto.setEvidence(appeal.getEvidence());
        dto.setStatus(appeal.getStatus());
        dto.setTeacherComment(appeal.getTeacherComment());
        dto.setSubmittedAt(appeal.getSubmittedAt());
        dto.setReviewedAt(appeal.getReviewedAt());
        return dto;
    }

    private String formatDayOfWeek(Course.DayOfWeek dayOfWeek) {
        Map<Course.DayOfWeek, String> map = new HashMap<>();
        map.put(Course.DayOfWeek.MONDAY, "周一");
        map.put(Course.DayOfWeek.TUESDAY, "周二");
        map.put(Course.DayOfWeek.WEDNESDAY, "周三");
        map.put(Course.DayOfWeek.THURSDAY, "周四");
        map.put(Course.DayOfWeek.FRIDAY, "周五");
        map.put(Course.DayOfWeek.SATURDAY, "周六");
        map.put(Course.DayOfWeek.SUNDAY, "周日");
        return map.getOrDefault(dayOfWeek, dayOfWeek.name());
    }
}
