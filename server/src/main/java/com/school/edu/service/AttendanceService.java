package com.school.edu.service;

import com.school.edu.dto.AttendanceDTO;
import com.school.edu.entity.*;
import com.school.edu.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendanceNotificationRepository notificationRepository;

    @Autowired
    private AttendanceAppealRepository appealRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

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

    @Transactional
    public AttendanceDTO createAttendance(AttendanceDTO dto, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("只有教师可以进行考勤");
        }

        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能对自己教授的课程进行考勤");
        }

        User student = userRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                student.getId(), course.getId(), Enrollment.EnrollmentStatus.ENROLLED)) {
            throw new RuntimeException("该学生未选修此课程");
        }

        if (attendanceRepository.existsByCourseIdAndAttendanceDateAndStudentId(
                course.getId(), dto.getAttendanceDate(), student.getId())) {
            throw new RuntimeException("该学生今日已有考勤记录");
        }

        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setCourse(course);
        attendance.setAttendanceDate(dto.getAttendanceDate());
        attendance.setStartSection(course.getStartSection());
        attendance.setEndSection(course.getEndSection());
        attendance.setStatus(dto.getStatus());
        attendance.setCreatedBy(teacher);
        attendance.setRemark(dto.getRemark());

        Attendance savedAttendance = attendanceRepository.save(attendance);

        if (dto.getStatus() == Attendance.AttendanceStatus.ABSENT) {
            createAbsentNotification(savedAttendance, course);
        }

        return toAttendanceDTO(savedAttendance);
    }

    @Transactional
    public List<AttendanceDTO> batchCreateAttendance(Long courseId, LocalDate attendanceDate, 
            List<Long> presentStudentIds, List<Long> absentStudentIds, List<Long> lateStudentIds, 
            String teacherUsername) {
        
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能对自己教授的课程进行考勤");
        }

        List<Enrollment> enrollments = enrollmentRepository.findEnrolledStudentsWithDetails(courseId);
        List<User> enrolledStudents = enrollments.stream()
                .map(Enrollment::getStudent)
                .collect(Collectors.toList());

        for (User student : enrolledStudents) {
            Attendance.AttendanceStatus status;
            if (presentStudentIds != null && presentStudentIds.contains(student.getId())) {
                status = Attendance.AttendanceStatus.PRESENT;
            } else if (absentStudentIds != null && absentStudentIds.contains(student.getId())) {
                status = Attendance.AttendanceStatus.ABSENT;
            } else if (lateStudentIds != null && lateStudentIds.contains(student.getId())) {
                status = Attendance.AttendanceStatus.LATE;
            } else {
                status = Attendance.AttendanceStatus.PRESENT;
            }

            java.util.Optional<Attendance> existingAttendanceOpt = attendanceRepository
                    .findByCourseIdAndDateAndStudentId(courseId, attendanceDate, student.getId());

            if (existingAttendanceOpt.isPresent()) {
                Attendance existingAttendance = existingAttendanceOpt.get();
                Attendance.AttendanceStatus oldStatus = existingAttendance.getStatus();
                existingAttendance.setStatus(status);
                Attendance savedAttendance = attendanceRepository.save(existingAttendance);
                
                if (oldStatus != Attendance.AttendanceStatus.ABSENT && oldStatus != Attendance.AttendanceStatus.LATE) {
                    if (status == Attendance.AttendanceStatus.ABSENT) {
                        createAbsentNotification(savedAttendance, course);
                    } else if (status == Attendance.AttendanceStatus.LATE) {
                        createLateNotification(savedAttendance, course);
                    }
                }
            } else {
                Attendance attendance = new Attendance();
                attendance.setStudent(student);
                attendance.setCourse(course);
                attendance.setAttendanceDate(attendanceDate);
                attendance.setStartSection(course.getStartSection());
                attendance.setEndSection(course.getEndSection());
                attendance.setStatus(status);
                attendance.setCreatedBy(teacher);

                Attendance savedAttendance = attendanceRepository.save(attendance);

                if (status == Attendance.AttendanceStatus.ABSENT) {
                    createAbsentNotification(savedAttendance, course);
                } else if (status == Attendance.AttendanceStatus.LATE) {
                    createLateNotification(savedAttendance, course);
                }
            }
        }

        return attendanceRepository.findByCourseIdAndDateWithDetails(courseId, attendanceDate)
                .stream()
                .map(this::toAttendanceDTO)
                .collect(Collectors.toList());
    }

    @Autowired
    private NotificationService notificationService;

    private void createAbsentNotification(Attendance attendance, Course course) {
        String message = String.format("您于 %s（%s）第%d-%d节课 %s 缺勤，请确认。如有异议请发起申诉。",
                attendance.getAttendanceDate().toString(),
                formatDayOfWeek(course.getDayOfWeek()),
                course.getStartSection(),
                course.getEndSection(),
                course.getCourseName());

        notificationService.createNotification(
            attendance.getStudent().getId(),
            Notification.NotificationType.ATTENDANCE_ABSENT,
            message,
            attendance.getId()
        );
    }

    private void createLateNotification(Attendance attendance, Course course) {
        String message = String.format("您于 %s（%s）第%d-%d节课 %s 迟到，请确认。",
                attendance.getAttendanceDate().toString(),
                formatDayOfWeek(course.getDayOfWeek()),
                course.getStartSection(),
                course.getEndSection(),
                course.getCourseName());

        notificationService.createNotification(
            attendance.getStudent().getId(),
            Notification.NotificationType.ATTENDANCE_ABSENT,
            message,
            attendance.getId()
        );
    }

    public List<AttendanceDTO> getTeacherAttendances(String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        return attendanceRepository.findByTeacherIdWithDetails(teacher.getId())
                .stream()
                .map(this::toAttendanceDTO)
                .collect(Collectors.toList());
    }

    public List<AttendanceDTO> getStudentAttendances(String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        return attendanceRepository.findByStudentIdWithDetails(student.getId())
                .stream()
                .map(this::toAttendanceDTO)
                .collect(Collectors.toList());
    }

    public List<AttendanceDTO> getCourseAttendances(Long courseId) {
        return attendanceRepository.findByCourseIdWithDetails(courseId)
                .stream()
                .map(this::toAttendanceDTO)
                .collect(Collectors.toList());
    }

    public List<AttendanceDTO> getCourseAttendancesByDate(Long courseId, LocalDate date) {
        return attendanceRepository.findByCourseIdAndDateWithDetails(courseId, date)
                .stream()
                .map(this::toAttendanceDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AttendanceDTO updateAttendanceStatus(Long id, Attendance.AttendanceStatus status, String remark) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("考勤记录不存在"));

        attendance.setStatus(status);
        attendance.setRemark(remark);

        return toAttendanceDTO(attendanceRepository.save(attendance));
    }

    @Transactional
    public void deleteAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("考勤记录不存在"));

        attendanceRepository.delete(attendance);
    }

    public List<AttendanceDTO> getAbsentStatisticsByCourse(Long courseId) {
        return attendanceRepository.findAbsentByCourseIdWithDetails(courseId)
                .stream()
                .map(this::toAttendanceDTO)
                .collect(Collectors.toList());
    }

    private AttendanceDTO toAttendanceDTO(Attendance attendance) {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setId(attendance.getId());
        dto.setStudentId(attendance.getStudent().getId());
        dto.setStudentName(attendance.getStudent().getName());
        dto.setStudentUsername(attendance.getStudent().getUsername());
        dto.setCourseId(attendance.getCourse().getId());
        dto.setCourseName(attendance.getCourse().getCourseName());
        dto.setAttendanceDate(attendance.getAttendanceDate());
        dto.setDayOfWeek(formatDayOfWeek(attendance.getCourse().getDayOfWeek()));
        dto.setStartSection(attendance.getStartSection());
        dto.setEndSection(attendance.getEndSection());
        dto.setStatus(attendance.getStatus());
        if (attendance.getCreatedBy() != null) {
            dto.setCreatedById(attendance.getCreatedBy().getId());
            dto.setCreatedByName(attendance.getCreatedBy().getName());
        }
        dto.setRemark(attendance.getRemark());

        boolean hasPendingAppeal = appealRepository.existsByAttendanceIdAndStatus(
                attendance.getId(), AttendanceAppeal.AppealStatus.PENDING);
        dto.setHasAppeal(hasPendingAppeal);

        return dto;
    }
}
