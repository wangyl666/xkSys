package com.school.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EduManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(EduManagementApplication.class, args);
    }
}