package com.school.edu.config;

import com.school.edu.entity.Course;
import com.school.edu.entity.User;
import com.school.edu.repository.CourseRepository;
import com.school.edu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initTeachers();
        initStudents();
    }

    private void initTeachers() {
        Optional<User> teacher1 = userRepository.findByUsername("teacher1");
        if (teacher1.isEmpty()) {
            User t1 = new User();
            t1.setUsername("teacher1");
            t1.setPassword(passwordEncoder.encode("123456"));
            t1.setName("张教授");
            t1.setRole(User.Role.TEACHER);
            t1.setDepartment("计算机科学与技术学院");
            t1.setEmail("zhang@school.edu.cn");
            userRepository.save(t1);
        }

        Optional<User> teacher2 = userRepository.findByUsername("teacher2");
        if (teacher2.isEmpty()) {
            User t2 = new User();
            t2.setUsername("teacher2");
            t2.setPassword(passwordEncoder.encode("123456"));
            t2.setName("李教授");
            t2.setRole(User.Role.TEACHER);
            t2.setDepartment("数学与统计学院");
            t2.setEmail("li@school.edu.cn");
            userRepository.save(t2);
        }

        Optional<User> teacher3 = userRepository.findByUsername("teacher3");
        if (teacher3.isEmpty()) {
            User t3 = new User();
            t3.setUsername("teacher3");
            t3.setPassword(passwordEncoder.encode("123456"));
            t3.setName("王教授");
            t3.setRole(User.Role.TEACHER);
            t3.setDepartment("外国语学院");
            t3.setEmail("wang@school.edu.cn");
            userRepository.save(t3);
        }
    }

    private void initStudents() {
        Optional<User> student1 = userRepository.findByUsername("student1");
        if (student1.isEmpty()) {
            User s1 = new User();
            s1.setUsername("student1");
            s1.setPassword(passwordEncoder.encode("123456"));
            s1.setName("小明");
            s1.setRole(User.Role.STUDENT);
            s1.setDepartment("计算机科学与技术学院");
            s1.setEmail("xiaoming@school.edu.cn");
            userRepository.save(s1);
        }

        Optional<User> student2 = userRepository.findByUsername("student2");
        if (student2.isEmpty()) {
            User s2 = new User();
            s2.setUsername("student2");
            s2.setPassword(passwordEncoder.encode("123456"));
            s2.setName("小红");
            s2.setRole(User.Role.STUDENT);
            s2.setDepartment("数学与统计学院");
            s2.setEmail("xiaohong@school.edu.cn");
            userRepository.save(s2);
        }
    }
}