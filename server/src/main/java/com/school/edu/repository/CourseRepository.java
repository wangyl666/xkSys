package com.school.edu.repository;

import com.school.edu.entity.Course;
import com.school.edu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacher(User teacher);
    List<Course> findByStatus(Course.CourseStatus status);
    List<Course> findByTeacherId(Long teacherId);
    List<Course> findBySemester(String semester);
}