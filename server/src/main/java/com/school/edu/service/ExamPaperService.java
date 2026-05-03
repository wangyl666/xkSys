package com.school.edu.service;

import com.school.edu.dto.ExamPaperDTO;
import com.school.edu.dto.ExamPaperQuestionDTO;
import com.school.edu.dto.QuestionDTO;
import com.school.edu.entity.*;
import com.school.edu.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamPaperService {

    @Autowired
    private ExamPaperRepository examPaperRepository;

    @Autowired
    private ExamPaperQuestionRepository examPaperQuestionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Random random = new Random();

    public List<ExamPaperDTO> getTeacherExamPapers(Long teacherId) {
        return examPaperRepository.findByTeacherId(teacherId).stream()
                .map(this::toExamPaperDTO)
                .collect(Collectors.toList());
    }

    public List<ExamPaperDTO> getExamPapersByCourse(Long teacherId, Long courseId) {
        return examPaperRepository.findByTeacherIdAndCourseId(teacherId, courseId).stream()
                .map(this::toExamPaperDTO)
                .collect(Collectors.toList());
    }

    public ExamPaperDTO getExamPaperById(Long id) {
        ExamPaper examPaper = examPaperRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("试卷不存在"));
        return toExamPaperDTO(examPaper);
    }

    @Transactional
    public ExamPaperDTO createExamPaper(ExamPaperDTO examPaperDTO, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("只有教师可以创建试卷");
        }

        ExamPaper examPaper = new ExamPaper();
        examPaper.setTeacher(teacher);
        examPaper.setTitle(examPaperDTO.getTitle());
        examPaper.setDescription(examPaperDTO.getDescription());
        examPaper.setTotalScore(examPaperDTO.getTotalScore() != null ? examPaperDTO.getTotalScore() : 0.0);
        examPaper.setDuration(examPaperDTO.getDuration());
        examPaper.setStatus(ExamPaper.ExamPaperStatus.DRAFT);
        examPaper.setCreationMethod(ExamPaper.CreationMethod.MANUAL);
        examPaper.setQuestionCount(examPaperDTO.getQuestions() != null ? examPaperDTO.getQuestions().size() : 0);

        if (examPaperDTO.getCourseId() != null) {
            Course course = courseRepository.findById(examPaperDTO.getCourseId())
                    .orElseThrow(() -> new RuntimeException("课程不存在"));
            examPaper.setCourse(course);
        }

        if (examPaperDTO.getQuestions() != null && !examPaperDTO.getQuestions().isEmpty()) {
            List<ExamPaperQuestion> paperQuestions = buildExamPaperQuestions(
                    examPaper, examPaperDTO.getQuestions());
            examPaper.setQuestions(paperQuestions);
        }

        ExamPaper savedExamPaper = examPaperRepository.save(examPaper);

        return toExamPaperDTO(savedExamPaper);
    }

    @Transactional
    public ExamPaperDTO autoGenerateExamPaper(ExamPaperDTO.AutoGenerateConfig config, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("只有教师可以创建试卷");
        }

        if (config.getTypeConfigs() == null || config.getTypeConfigs().isEmpty()) {
            throw new RuntimeException("请配置题目类型和数量");
        }

        List<Question> selectedQuestions = new ArrayList<>();
        Map<Long, Double> questionScores = new HashMap<>();
        double totalScore = 0.0;

        for (ExamPaperDTO.QuestionTypeConfig typeConfig : config.getTypeConfigs()) {
            List<Question> availableQuestions = findAvailableQuestions(
                    teacher.getId(), config.getCourseId(), typeConfig.getType(), typeConfig.getDifficulty());

            if (availableQuestions.size() < typeConfig.getCount()) {
                throw new RuntimeException(
                        String.format("%s题目数量不足，需要%d题，现有%d题",
                                typeConfig.getType().getDescription(),
                                typeConfig.getCount(),
                                availableQuestions.size()));
            }

            Collections.shuffle(availableQuestions, random);
            List<Question> selected = availableQuestions.subList(0, typeConfig.getCount());

            for (Question q : selected) {
                questionScores.put(q.getId(), typeConfig.getScorePerQuestion());
                totalScore += typeConfig.getScorePerQuestion();
            }
            selectedQuestions.addAll(selected);
        }

        ExamPaper examPaper = new ExamPaper();
        examPaper.setTeacher(teacher);
        examPaper.setTitle("自动生成试卷 - " + new java.text.SimpleDateFormat("yyyyMMddHHmm").format(new Date()));
        examPaper.setDescription("由系统自动生成的试卷");
        examPaper.setTotalScore(totalScore);
        examPaper.setDuration(config.getDuration() != null ? config.getDuration() : 120);
        examPaper.setStatus(ExamPaper.ExamPaperStatus.DRAFT);
        examPaper.setCreationMethod(ExamPaper.CreationMethod.AUTO);

        if (config.getCourseId() != null) {
            Course course = courseRepository.findById(config.getCourseId())
                    .orElseThrow(() -> new RuntimeException("课程不存在"));
            examPaper.setCourse(course);
        }

        ExamPaper savedExamPaper = examPaperRepository.save(examPaper);

        List<ExamPaperQuestion> paperQuestions = new ArrayList<>();
        for (int i = 0; i < selectedQuestions.size(); i++) {
            ExamPaperQuestion eq = new ExamPaperQuestion();
            eq.setExamPaper(savedExamPaper);
            eq.setQuestion(selectedQuestions.get(i));
            eq.setSortOrder(i + 1);
            eq.setScore(questionScores.get(selectedQuestions.get(i).getId()));
            paperQuestions.add(eq);
        }

        savedExamPaper.setQuestions(paperQuestions);
        savedExamPaper.setQuestionCount(paperQuestions.size());
        savedExamPaper = examPaperRepository.save(savedExamPaper);

        return toExamPaperDTO(savedExamPaper);
    }

    @Transactional
    public ExamPaperDTO updateExamPaper(Long id, ExamPaperDTO examPaperDTO, String teacherUsername) {
        ExamPaper examPaper = examPaperRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("试卷不存在"));

        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (!examPaper.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能修改自己创建的试卷");
        }

        if (examPaper.getStatus() == ExamPaper.ExamPaperStatus.PUBLISHED) {
            throw new RuntimeException("已发布的试卷不能修改");
        }

        examPaper.setTitle(examPaperDTO.getTitle());
        examPaper.setDescription(examPaperDTO.getDescription());
        examPaper.setTotalScore(examPaperDTO.getTotalScore());
        examPaper.setDuration(examPaperDTO.getDuration());

        if (examPaperDTO.getCourseId() != null && !examPaperDTO.getCourseId().equals(
                examPaper.getCourse() != null ? examPaper.getCourse().getId() : null)) {
            Course course = courseRepository.findById(examPaperDTO.getCourseId())
                    .orElseThrow(() -> new RuntimeException("课程不存在"));
            examPaper.setCourse(course);
        }

        if (examPaperDTO.getQuestions() != null) {
            examPaper.getQuestions().clear();
            examPaperQuestionRepository.flush();

            if (!examPaperDTO.getQuestions().isEmpty()) {
                List<ExamPaperQuestion> paperQuestions = buildExamPaperQuestions(
                        examPaper, examPaperDTO.getQuestions());
                examPaper.setQuestions(paperQuestions);
                examPaper.setQuestionCount(paperQuestions.size());
            } else {
                examPaper.setQuestionCount(0);
            }
        }

        ExamPaper savedExamPaper = examPaperRepository.save(examPaper);
        return toExamPaperDTO(savedExamPaper);
    }

    @Transactional
    public void deleteExamPaper(Long id, String teacherUsername) {
        ExamPaper examPaper = examPaperRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("试卷不存在"));

        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (!examPaper.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能删除自己创建的试卷");
        }

        if (examPaper.getStatus() == ExamPaper.ExamPaperStatus.PUBLISHED) {
            throw new RuntimeException("已发布的试卷不能删除");
        }

        examPaperRepository.delete(examPaper);
    }

    @Transactional
    public ExamPaperDTO publishExamPaper(Long id, String teacherUsername) {
        ExamPaper examPaper = examPaperRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("试卷不存在"));

        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (!examPaper.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能发布自己创建的试卷");
        }

        if (examPaper.getQuestions() == null || examPaper.getQuestions().isEmpty()) {
            throw new RuntimeException("试卷中没有题目，无法发布");
        }

        examPaper.setStatus(ExamPaper.ExamPaperStatus.PUBLISHED);
        ExamPaper savedExamPaper = examPaperRepository.save(examPaper);
        return toExamPaperDTO(savedExamPaper);
    }

    private List<Question> findAvailableQuestions(Long teacherId, Long courseId,
                                                   Question.QuestionType type, String difficulty) {
        List<Question> questions;

        if (courseId != null) {
            questions = questionRepository.findByTeacherIdAndCourseId(teacherId, courseId);
        } else {
            questions = questionRepository.findByTeacherId(teacherId);
        }

        return questions.stream()
                .filter(q -> q.getType() == type)
                .filter(q -> difficulty == null || difficulty.isEmpty() || difficulty.equals(q.getDifficulty()))
                .collect(Collectors.toList());
    }

    private List<ExamPaperQuestion> buildExamPaperQuestions(
            ExamPaper examPaper, List<ExamPaperQuestionDTO> questionDTOs) {
        List<ExamPaperQuestion> paperQuestions = new ArrayList<>();

        for (int i = 0; i < questionDTOs.size(); i++) {
            ExamPaperQuestionDTO dto = questionDTOs.get(i);
            Question question = questionRepository.findById(dto.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("题目不存在: " + dto.getQuestionId()));

            ExamPaperQuestion eq = new ExamPaperQuestion();
            eq.setExamPaper(examPaper);
            eq.setQuestion(question);
            eq.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : (i + 1));
            eq.setScore(dto.getScore() != null ? dto.getScore() : question.getScore());
            paperQuestions.add(eq);
        }

        return paperQuestions;
    }

    private ExamPaperDTO toExamPaperDTO(ExamPaper examPaper) {
        ExamPaperDTO dto = new ExamPaperDTO();
        dto.setId(examPaper.getId());
        dto.setTitle(examPaper.getTitle());
        dto.setDescription(examPaper.getDescription());
        dto.setTotalScore(examPaper.getTotalScore());
        dto.setQuestionCount(examPaper.getQuestionCount());
        dto.setDuration(examPaper.getDuration());
        dto.setStatus(examPaper.getStatus());
        dto.setStatusName(examPaper.getStatus() != null ? examPaper.getStatus().getDescription() : null);
        dto.setCreationMethod(examPaper.getCreationMethod());
        dto.setCreationMethodName(examPaper.getCreationMethod() != null ?
                examPaper.getCreationMethod().getDescription() : null);

        dto.setTeacherId(examPaper.getTeacher().getId());
        dto.setTeacherName(examPaper.getTeacher().getName());

        if (examPaper.getCourse() != null) {
            dto.setCourseId(examPaper.getCourse().getId());
            dto.setCourseName(examPaper.getCourse().getCourseName());
        }

        if (examPaper.getCreatedAt() != null) {
            dto.setCreatedAt(examPaper.getCreatedAt().format(formatter));
        }
        if (examPaper.getUpdatedAt() != null) {
            dto.setUpdatedAt(examPaper.getUpdatedAt().format(formatter));
        }

        if (examPaper.getQuestions() != null && !examPaper.getQuestions().isEmpty()) {
            dto.setQuestions(examPaper.getQuestions().stream()
                    .sorted(Comparator.comparing(ExamPaperQuestion::getSortOrder))
                    .map(this::toExamPaperQuestionDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private ExamPaperQuestionDTO toExamPaperQuestionDTO(ExamPaperQuestion eq) {
        ExamPaperQuestionDTO dto = new ExamPaperQuestionDTO();
        dto.setId(eq.getId());
        dto.setExamPaperId(eq.getExamPaper().getId());
        dto.setQuestionId(eq.getQuestion().getId());
        dto.setSortOrder(eq.getSortOrder());
        dto.setScore(eq.getScore());

        Question q = eq.getQuestion();
        dto.setType(q.getType());
        dto.setTypeName(q.getType().getDescription());
        dto.setContent(q.getContent());
        dto.setOptions(q.getOptions());
        dto.setAnswer(q.getAnswer());
        dto.setExplanation(q.getExplanation());
        dto.setDifficulty(q.getDifficulty());

        return dto;
    }
}
