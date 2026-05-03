package com.school.edu.service;

import com.school.edu.dto.QuestionDTO;
import com.school.edu.entity.Course;
import com.school.edu.entity.Question;
import com.school.edu.entity.User;
import com.school.edu.repository.CourseRepository;
import com.school.edu.repository.QuestionRepository;
import com.school.edu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<QuestionDTO> getTeacherQuestions(Long teacherId) {
        return questionRepository.findByTeacherId(teacherId).stream()
                .map(this::toQuestionDTO)
                .collect(Collectors.toList());
    }

    public List<QuestionDTO> getQuestionsByCourse(Long teacherId, Long courseId) {
        return questionRepository.findByTeacherIdAndCourseId(teacherId, courseId).stream()
                .map(this::toQuestionDTO)
                .collect(Collectors.toList());
    }

    public List<QuestionDTO> getPublicQuestions(Long teacherId) {
        return questionRepository.findByTeacherIdOrIsPublicTrue(teacherId).stream()
                .map(this::toQuestionDTO)
                .collect(Collectors.toList());
    }

    public QuestionDTO getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));
        return toQuestionDTO(question);
    }

    @Transactional
    public QuestionDTO createQuestion(QuestionDTO questionDTO, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("只有教师可以创建题目");
        }

        Question question = new Question();
        question.setTeacher(teacher);
        question.setType(questionDTO.getType());
        question.setContent(questionDTO.getContent());
        question.setOptions(questionDTO.getOptions());
        question.setAnswer(questionDTO.getAnswer());
        question.setScore(questionDTO.getScore() != null ? questionDTO.getScore() : 1.0);
        question.setExplanation(questionDTO.getExplanation());
        question.setTag(questionDTO.getTag());
        question.setDifficulty(questionDTO.getDifficulty());
        question.setIsPublic(questionDTO.getIsPublic() != null ? questionDTO.getIsPublic() : false);

        if (questionDTO.getCourseId() != null) {
            Course course = courseRepository.findById(questionDTO.getCourseId())
                    .orElseThrow(() -> new RuntimeException("课程不存在"));
            question.setCourse(course);
        }

        Question savedQuestion = questionRepository.save(question);
        return toQuestionDTO(savedQuestion);
    }

    @Transactional
    public QuestionDTO updateQuestion(Long id, QuestionDTO questionDTO, String teacherUsername) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));

        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (!question.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能修改自己创建的题目");
        }

        question.setType(questionDTO.getType());
        question.setContent(questionDTO.getContent());
        question.setOptions(questionDTO.getOptions());
        question.setAnswer(questionDTO.getAnswer());
        question.setScore(questionDTO.getScore() != null ? questionDTO.getScore() : question.getScore());
        question.setExplanation(questionDTO.getExplanation());
        question.setTag(questionDTO.getTag());
        question.setDifficulty(questionDTO.getDifficulty());
        question.setIsPublic(questionDTO.getIsPublic() != null ? questionDTO.getIsPublic() : question.getIsPublic());

        if (questionDTO.getCourseId() != null && !questionDTO.getCourseId().equals(
                question.getCourse() != null ? question.getCourse().getId() : null)) {
            Course course = courseRepository.findById(questionDTO.getCourseId())
                    .orElseThrow(() -> new RuntimeException("课程不存在"));
            question.setCourse(course);
        }

        Question savedQuestion = questionRepository.save(question);
        return toQuestionDTO(savedQuestion);
    }

    @Transactional
    public void deleteQuestion(Long id, String teacherUsername) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("题目不存在"));

        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (!question.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能删除自己创建的题目");
        }

        questionRepository.delete(question);
    }

    @Transactional
    public List<QuestionDTO> batchCreateQuestions(List<QuestionDTO> questionDTOs, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("只有教师可以创建题目");
        }

        return questionDTOs.stream()
                .map(dto -> {
                    Question question = new Question();
                    question.setTeacher(teacher);
                    question.setType(dto.getType());
                    question.setContent(dto.getContent());
                    question.setOptions(dto.getOptions());
                    question.setAnswer(dto.getAnswer());
                    question.setScore(dto.getScore() != null ? dto.getScore() : 1.0);
                    question.setExplanation(dto.getExplanation());
                    question.setTag(dto.getTag());
                    question.setDifficulty(dto.getDifficulty());
                    question.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false);

                    if (dto.getCourseId() != null) {
                        courseRepository.findById(dto.getCourseId()).ifPresent(question::setCourse);
                    }

                    return questionRepository.save(question);
                })
                .map(this::toQuestionDTO)
                .collect(Collectors.toList());
    }

    private QuestionDTO toQuestionDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setType(question.getType());
        dto.setTypeName(question.getType().getDescription());
        dto.setContent(question.getContent());
        dto.setOptions(question.getOptions());
        dto.setAnswer(question.getAnswer());
        dto.setScore(question.getScore());
        dto.setExplanation(question.getExplanation());
        dto.setTag(question.getTag());
        dto.setDifficulty(question.getDifficulty());
        dto.setIsPublic(question.getIsPublic());
        dto.setTeacherId(question.getTeacher().getId());
        dto.setTeacherName(question.getTeacher().getName());

        if (question.getCourse() != null) {
            dto.setCourseId(question.getCourse().getId());
            dto.setCourseName(question.getCourse().getCourseName());
        }

        if (question.getCreatedAt() != null) {
            dto.setCreatedAt(question.getCreatedAt().format(formatter));
        }
        if (question.getUpdatedAt() != null) {
            dto.setUpdatedAt(question.getUpdatedAt().format(formatter));
        }

        return dto;
    }
}
