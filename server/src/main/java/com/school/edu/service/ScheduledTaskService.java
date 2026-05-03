package com.school.edu.service;

import com.school.edu.entity.*;
import com.school.edu.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ScheduledTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkHomeworkReminders() {
        logger.info("开始检查作业提醒任务执行时间: {}", LocalDateTime.now());
        
        LocalDateTime now = LocalDateTime.now();
        
        List<Homework> homeworks = homeworkRepository.findUpcomingWithReminderEnabled(now);
        
        for (Homework homework : homeworks) {
            if (homework.getDeadline() == null || !homework.getEnableReminder() || homework.getReminderSent()) {
                continue;
            }
            
            long minutesUntilDeadline = ChronoUnit.MINUTES.between(now, homework.getDeadline());
            
            if (minutesUntilDeadline >= 0 && minutesUntilDeadline <= homework.getReminderMinutes()) {
                sendHomeworkReminder(homework, minutesUntilDeadline);
                homework.setReminderSent(true);
                homeworkRepository.save(homework);
            }
        }
    }

    private void sendHomeworkReminder(Homework homework, long minutesUntilDeadline) {
        List<Enrollment> enrollments = enrollmentRepository.findEnrolledStudentsWithDetails(homework.getCourse().getId());
        
        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getStudent();
            
            String timeText;
            if (minutesUntilDeadline < 1) {
                timeText = "即将";
            } else if (minutesUntilDeadline < 60) {
                timeText = minutesUntilDeadline + "分钟后";
            } else {
                long hours = minutesUntilDeadline / 60;
                timeText = hours + "小时后";
            }
            
            String message = String.format("您的作业【" + homework.getTitle() + "】" + timeText + 
                "截止，请及时提交！课程：" + homework.getCourse().getCourseName());
            
            try {
                notificationService.createNotification(
                    student.getId(),
                    Notification.NotificationType.HOMEWORK_REMINDER,
                    message,
                    homework.getId()
                );
                logger.info("已发送作业提醒给学生: {}, 作业: {}", student.getUsername(), homework.getTitle());
            } catch (Exception e) {
                logger.error("发送作业提醒失败: {}", e.getMessage());
            }
        }
    }
}
