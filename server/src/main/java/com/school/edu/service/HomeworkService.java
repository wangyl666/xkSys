package com.school.edu.service;

import com.school.edu.dto.HomeworkDTO;
import com.school.edu.dto.HomeworkSubmissionDTO;
import com.school.edu.entity.*;
import com.school.edu.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HomeworkService {

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private HomeworkSubmissionRepository submissionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Transactional
    public HomeworkDTO createHomework(HomeworkDTO dto, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("只有教师可以发布作业");
        }

        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能为自己教授的课程发布作业");
        }

        Homework homework = new Homework();
        homework.setCourse(course);
        homework.setTeacher(teacher);
        homework.setTitle(dto.getTitle());
        homework.setDescription(dto.getDescription());
        homework.setDeadline(dto.getDeadline());
        homework.setEnableReminder(dto.getEnableReminder() != null ? dto.getEnableReminder() : true);
        homework.setReminderMinutes(dto.getReminderMinutes() != null ? dto.getReminderMinutes() : 3);
        homework.setStatus(dto.getStatus() != null ? dto.getStatus() : Homework.HomeworkStatus.DRAFT);

        Homework savedHomework = homeworkRepository.save(homework);

        if (savedHomework.getStatus() == Homework.HomeworkStatus.PUBLISHED) {
            createSubmissionsForHomework(savedHomework);
        }

        return toHomeworkDTO(savedHomework);
    }

    @Transactional
    public HomeworkDTO updateHomework(Long id, HomeworkDTO dto, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        Homework homework = homeworkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("作业不存在"));

        if (!homework.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能编辑自己发布的作业");
        }

        boolean wasDraft = homework.getStatus() == Homework.HomeworkStatus.DRAFT;

        if (dto.getTitle() != null) {
            homework.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            homework.setDescription(dto.getDescription());
        }
        if (dto.getDeadline() != null) {
            homework.setDeadline(dto.getDeadline());
        }
        if (dto.getEnableReminder() != null) {
            homework.setEnableReminder(dto.getEnableReminder());
        }
        if (dto.getReminderMinutes() != null) {
            homework.setReminderMinutes(dto.getReminderMinutes());
        }
        if (dto.getStatus() != null) {
            homework.setStatus(dto.getStatus());
        }

        Homework savedHomework = homeworkRepository.save(homework);

        if (wasDraft && savedHomework.getStatus() == Homework.HomeworkStatus.PUBLISHED) {
            createSubmissionsForHomework(savedHomework);
        }

        return toHomeworkDTO(savedHomework);
    }

    @Transactional
    public HomeworkDTO publishHomework(Long id, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        Homework homework = homeworkRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("作业不存在"));

        if (!homework.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能发布自己的作业");
        }

        if (homework.getStatus() == Homework.HomeworkStatus.PUBLISHED) {
            return toHomeworkDTO(homework);
        }

        homework.setStatus(Homework.HomeworkStatus.PUBLISHED);
        Homework savedHomework = homeworkRepository.save(homework);

        createSubmissionsForHomework(savedHomework);

        return toHomeworkDTO(savedHomework);
    }

    private void createSubmissionsForHomework(Homework homework) {
        List<Enrollment> enrollments = enrollmentRepository.findEnrolledStudentsWithDetails(homework.getCourse().getId());
        
        for (Enrollment enrollment : enrollments) {
            boolean exists = submissionRepository.findByHomeworkIdAndStudentId(
                    homework.getId(), enrollment.getStudent().getId()).isPresent();
            
            if (!exists) {
                HomeworkSubmission submission = new HomeworkSubmission();
                submission.setHomework(homework);
                submission.setStudent(enrollment.getStudent());
                submission.setStatus(HomeworkSubmission.SubmissionStatus.NOT_SUBMITTED);
                submissionRepository.save(submission);
            }
        }
    }

    public List<HomeworkDTO> getTeacherHomeworks(String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        return homeworkRepository.findByTeacherIdWithDetails(teacher.getId())
                .stream()
                .map(this::toHomeworkDTO)
                .collect(Collectors.toList());
    }

    public List<HomeworkDTO> getCourseHomeworks(Long courseId) {
        return homeworkRepository.findByCourseIdWithDetails(courseId)
                .stream()
                .map(this::toHomeworkDTO)
                .collect(Collectors.toList());
    }

    public HomeworkDTO getHomeworkById(Long id) {
        Homework homework = homeworkRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("作业不存在"));
        return toHomeworkDTO(homework);
    }

    public List<HomeworkSubmissionDTO> getHomeworkSubmissions(Long homeworkId) {
        return submissionRepository.findByHomeworkIdWithDetails(homeworkId)
                .stream()
                .map(this::toHomeworkSubmissionDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public HomeworkSubmissionDTO submitHomework(Long homeworkId, String content, String fileUrl, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new RuntimeException("作业不存在"));

        if (homework.getStatus() != Homework.HomeworkStatus.PUBLISHED) {
            throw new RuntimeException("该作业尚未发布");
        }

        if (!enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                student.getId(), homework.getCourse().getId(), Enrollment.EnrollmentStatus.ENROLLED)) {
            throw new RuntimeException("您未选修此课程");
        }

        HomeworkSubmission submission = submissionRepository.findByHomeworkIdAndStudentId(homeworkId, student.getId())
                .orElseGet(() -> {
                    HomeworkSubmission newSubmission = new HomeworkSubmission();
                    newSubmission.setHomework(homework);
                    newSubmission.setStudent(student);
                    newSubmission.setStatus(HomeworkSubmission.SubmissionStatus.NOT_SUBMITTED);
                    return newSubmission;
                });

        boolean isLate = LocalDateTime.now().isAfter(homework.getDeadline());

        submission.setContent(content);
        submission.setFileUrl(fileUrl);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setIsLate(isLate);
        submission.setStatus(HomeworkSubmission.SubmissionStatus.SUBMITTED);

        return toHomeworkSubmissionDTO(submissionRepository.save(submission));
    }

    @Transactional
    public HomeworkSubmissionDTO gradeHomework(Long submissionId, Double score, String teacherComment, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        HomeworkSubmission submission = submissionRepository.findByIdWithDetails(submissionId)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));

        if (!submission.getHomework().getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能评分自己课程的作业");
        }

        submission.setScore(score);
        submission.setTeacherComment(teacherComment);
        submission.setGradedAt(LocalDateTime.now());
        submission.setStatus(HomeworkSubmission.SubmissionStatus.GRADED);

        return toHomeworkSubmissionDTO(submissionRepository.save(submission));
    }

    public List<HomeworkDTO> getStudentHomeworks(String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        List<Enrollment> enrollments = enrollmentRepository.findEnrolledCoursesWithDetails(student.getId());
        List<Long> courseIds = enrollments.stream()
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toList());

        if (courseIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Homework> homeworks = homeworkRepository.findPublishedByCourseIds(courseIds);
        List<Long> homeworkIds = homeworks.stream().map(Homework::getId).collect(Collectors.toList());

        List<HomeworkSubmission> submissions = submissionRepository.findByStudentIdAndHomeworkIds(student.getId(), homeworkIds);
        Map<Long, HomeworkSubmission> submissionMap = submissions.stream()
                .collect(Collectors.toMap(s -> s.getHomework().getId(), s -> s));

        return homeworks.stream()
                .map(h -> toHomeworkDTOWithSubmissionStatus(h, submissionMap.get(h.getId())))
                .collect(Collectors.toList());
    }

    public HomeworkSubmissionDTO getStudentSubmission(Long homeworkId, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        return submissionRepository.findByHomeworkIdAndStudentId(homeworkId, student.getId())
                .map(this::toHomeworkSubmissionDTO)
                .orElse(null);
    }

    private HomeworkDTO toHomeworkDTO(Homework homework) {
        HomeworkDTO dto = new HomeworkDTO();
        dto.setId(homework.getId());
        dto.setCourseId(homework.getCourse().getId());
        dto.setCourseName(homework.getCourse().getCourseName());
        dto.setTeacherId(homework.getTeacher().getId());
        dto.setTeacherName(homework.getTeacher().getName());
        dto.setTitle(homework.getTitle());
        dto.setDescription(homework.getDescription());
        dto.setDeadline(homework.getDeadline());
        dto.setEnableReminder(homework.getEnableReminder());
        dto.setReminderMinutes(homework.getReminderMinutes());
        dto.setStatus(homework.getStatus());
        dto.setCreatedAt(homework.getCreatedAt());
        dto.setUpdatedAt(homework.getUpdatedAt());

        List<Enrollment> enrollments = enrollmentRepository.findByCourseIdAndStatus(
                homework.getCourse().getId(), Enrollment.EnrollmentStatus.ENROLLED);
        dto.setTotalStudents(enrollments.size());

        dto.setSubmittedCount(submissionRepository.countSubmittedByHomeworkId(homework.getId()).intValue());
        dto.setGradedCount(submissionRepository.countGradedByHomeworkId(homework.getId()).intValue());

        return dto;
    }

    private HomeworkDTO toHomeworkDTOWithSubmissionStatus(Homework homework, HomeworkSubmission submission) {
        HomeworkDTO dto = new HomeworkDTO();
        dto.setId(homework.getId());
        dto.setCourseId(homework.getCourse().getId());
        dto.setCourseName(homework.getCourse().getCourseName());
        dto.setTeacherId(homework.getTeacher().getId());
        dto.setTeacherName(homework.getTeacher().getName());
        dto.setTitle(homework.getTitle());
        dto.setDescription(homework.getDescription());
        dto.setDeadline(homework.getDeadline());
        dto.setEnableReminder(homework.getEnableReminder());
        dto.setReminderMinutes(homework.getReminderMinutes());
        dto.setStatus(homework.getStatus());
        dto.setCreatedAt(homework.getCreatedAt());
        dto.setUpdatedAt(homework.getUpdatedAt());

        return dto;
    }

    private HomeworkSubmissionDTO toHomeworkSubmissionDTO(HomeworkSubmission submission) {
        HomeworkSubmissionDTO dto = new HomeworkSubmissionDTO();
        dto.setId(submission.getId());
        dto.setHomeworkId(submission.getHomework().getId());
        dto.setHomeworkTitle(submission.getHomework().getTitle());
        dto.setHomeworkDeadline(submission.getHomework().getDeadline());
        dto.setStudentId(submission.getStudent().getId());
        dto.setStudentName(submission.getStudent().getName());
        dto.setStudentUsername(submission.getStudent().getUsername());
        dto.setContent(submission.getContent());
        dto.setFileUrl(submission.getFileUrl());
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setUpdatedAt(submission.getUpdatedAt());
        dto.setIsLate(submission.getIsLate());
        dto.setScore(submission.getScore());
        dto.setTeacherComment(submission.getTeacherComment());
        dto.setGradedAt(submission.getGradedAt());
        dto.setStatus(submission.getStatus());
        return dto;
    }
}
