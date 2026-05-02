package com.school.edu.service;

import com.school.edu.dto.CourseDTO;
import com.school.edu.entity.Course;
import com.school.edu.entity.User;
import com.school.edu.repository.CourseRepository;
import com.school.edu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::toCourseDTO)
                .collect(Collectors.toList());
    }

    public List<CourseDTO> getTeacherCourses(Long teacherId) {
        return courseRepository.findByTeacherId(teacherId).stream()
                .map(this::toCourseDTO)
                .collect(Collectors.toList());
    }

    public CourseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        return toCourseDTO(course);
    }

    @Transactional
    public CourseDTO createCourse(CourseDTO courseDTO, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("只有教师可以发布课程");
        }

        Course course = new Course();
        course.setCourseName(courseDTO.getCourseName());
        course.setDescription(courseDTO.getDescription());
        course.setTeacher(teacher);
        course.setMaxStudents(courseDTO.getMaxStudents());
        course.setEnrolledStudents(0);
        course.setCredits(courseDTO.getCredits());
        course.setDayOfWeek(courseDTO.getDayOfWeek());
        course.setStartSection(courseDTO.getStartSection());
        course.setEndSection(courseDTO.getEndSection());
        course.setLocation(courseDTO.getLocation());
        course.setSemester(courseDTO.getSemester());
        course.setStatus(Course.CourseStatus.AVAILABLE);

        Course savedCourse = courseRepository.save(course);
        return toCourseDTO(savedCourse);
    }

    @Transactional
    public CourseDTO updateCourse(Long id, CourseDTO courseDTO, String teacherUsername) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能修改自己发布的课程");
        }

        course.setCourseName(courseDTO.getCourseName());
        course.setDescription(courseDTO.getDescription());
        course.setMaxStudents(courseDTO.getMaxStudents());
        course.setCredits(courseDTO.getCredits());
        course.setDayOfWeek(courseDTO.getDayOfWeek());
        course.setStartSection(courseDTO.getStartSection());
        course.setEndSection(courseDTO.getEndSection());
        course.setLocation(courseDTO.getLocation());
        course.setSemester(courseDTO.getSemester());

        if (course.getEnrolledStudents() >= course.getMaxStudents()) {
            course.setStatus(Course.CourseStatus.FULL);
        }

        Course savedCourse = courseRepository.save(course);
        return toCourseDTO(savedCourse);
    }

    @Transactional
    public void deleteCourse(Long id, String teacherUsername) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("课程不存在"));

        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (!course.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能删除自己发布的课程");
        }

        if (course.getEnrolledStudents() > 0) {
            throw new RuntimeException("已有学生选课，无法删除课程");
        }

        courseRepository.delete(course);
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