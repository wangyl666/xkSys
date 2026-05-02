package com.school.edu.service;

import com.school.edu.dto.CourseDTO;
import com.school.edu.entity.Course;
import com.school.edu.entity.Enrollment;
import com.school.edu.entity.User;
import com.school.edu.repository.CourseRepository;
import com.school.edu.repository.EnrollmentRepository;
import com.school.edu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    public List<CourseDTO> getEnrolledCourses(String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        List<Enrollment> enrollments = enrollmentRepository.findEnrolledCoursesWithDetails(student.getId());
        
        return enrollments.stream()
                .map(e -> toCourseDTO(e.getCourse()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void enrollCourse(Long courseId, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        if (enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                student.getId(), courseId, Enrollment.EnrollmentStatus.ENROLLED)) {
            throw new RuntimeException("您已选择此课程");
        }

        if (course.getStatus() == Course.CourseStatus.CLOSED) {
            throw new RuntimeException("课程已关闭");
        }

        if (course.getEnrolledStudents() >= course.getMaxStudents()) {
            throw new RuntimeException("课程已满");
        }

        checkTimeConflict(student.getId(), course);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(Enrollment.EnrollmentStatus.ENROLLED);
        enrollmentRepository.save(enrollment);

        course.setEnrolledStudents(course.getEnrolledStudents() + 1);
        if (course.getEnrolledStudents() >= course.getMaxStudents()) {
            course.setStatus(Course.CourseStatus.FULL);
        }
        courseRepository.save(course);
    }

    @Transactional
    public void dropCourse(Long courseId, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndStatus(
                student, Enrollment.EnrollmentStatus.ENROLLED);

        Enrollment enrollment = enrollments.stream()
                .filter(e -> e.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到该选课记录"));

        enrollment.setStatus(Enrollment.EnrollmentStatus.DROPPED);
        enrollment.setDroppedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        Course course = enrollment.getCourse();
        course.setEnrolledStudents(course.getEnrolledStudents() - 1);
        if (course.getStatus() == Course.CourseStatus.FULL && 
            course.getEnrolledStudents() < course.getMaxStudents()) {
            course.setStatus(Course.CourseStatus.AVAILABLE);
        }
        courseRepository.save(course);
    }

    private void checkTimeConflict(Long studentId, Course newCourse) {
        List<Course> enrolledCourses = enrollmentRepository.findEnrolledCoursesWithDetails(studentId)
                .stream()
                .map(Enrollment::getCourse)
                .collect(Collectors.toList());

        for (Course enrolled : enrolledCourses) {
            if (enrolled.getDayOfWeek() == newCourse.getDayOfWeek()) {
                if (!(newCourse.getEndSection() < enrolled.getStartSection() || 
                      newCourse.getStartSection() > enrolled.getEndSection())) {
                    throw new RuntimeException("课程时间冲突：与 " + enrolled.getCourseName() + " 时间冲突");
                }
            }
        }
    }

    private CourseDTO toCourseDTO(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setCourseName(course.getCourseName());
        dto.setDescription(course.getDescription());
        dto.setMaxStudents(course.getMaxStudents());
        dto.setEnrolledStudents(course.getEnrolledStudents());
        dto.setCredits(course.getCredits());
        dto.setDayOfWeek(course.getDayOfWeek());
        dto.setStartSection(course.getStartSection());
        dto.setEndSection(course.getEndSection());
        dto.setLocation(course.getLocation());
        dto.setSemester(course.getSemester());
        dto.setStatus(course.getStatus());
        dto.setTeacherId(course.getTeacher().getId());
        dto.setTeacherName(course.getTeacher().getName());
        return dto;
    }
}