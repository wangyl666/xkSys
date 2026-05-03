package com.school.edu.service;

import com.school.edu.dto.ExamAnswerDTO;
import com.school.edu.dto.ExamDTO;
import com.school.edu.dto.ExamPaperQuestionDTO;
import com.school.edu.dto.ExamSubmissionDTO;
import com.school.edu.entity.*;
import com.school.edu.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamSubmissionRepository submissionRepository;

    @Autowired
    private ExamAnswerRepository answerRepository;

    @Autowired
    private ExamPaperRepository examPaperRepository;

    @Autowired
    private ExamPaperQuestionRepository examPaperQuestionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<ExamDTO> getTeacherExams(Long teacherId, Long courseId) {
        List<Exam> exams;
        if (courseId != null) {
            exams = examRepository.findByTeacherIdAndCourseId(teacherId, courseId);
        } else {
            exams = examRepository.findByTeacherId(teacherId);
        }
        return exams.stream().map(this::toExamDTO).collect(Collectors.toList());
    }

    public ExamDTO getExamById(Long id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("考试不存在"));
        return toExamDTO(exam);
    }

    public ExamDTO getExamForStudent(Long examId, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("考试不存在"));

        if (exam.getStatus() != Exam.ExamStatus.PUBLISHED && exam.getStatus() != Exam.ExamStatus.IN_PROGRESS) {
            throw new RuntimeException("该考试尚未开放");
        }

        if (exam.getCourse() != null) {
            if (!enrollmentRepository.existsByStudentIdAndCourseIdAndStatus(
                    student.getId(), exam.getCourse().getId(), Enrollment.EnrollmentStatus.ENROLLED)) {
                throw new RuntimeException("您未选修此课程");
            }
        }

        ExamDTO examDTO = toExamDTO(exam);

        Optional<ExamSubmission> submissionOpt = submissionRepository.findByExamIdAndStudentId(examId, student.getId());
        if (submissionOpt.isPresent()) {
            examDTO.setMySubmission(toExamSubmissionDTO(submissionOpt.get()));
        }

        return examDTO;
    }

    @Transactional
    public ExamDTO createExam(ExamDTO examDTO, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        if (teacher.getRole() != User.Role.TEACHER) {
            throw new RuntimeException("只有教师可以创建考试");
        }

        ExamPaper examPaper = null;
        if (examDTO.getExamPaperId() != null) {
            examPaper = examPaperRepository.findById(examDTO.getExamPaperId())
                    .orElseThrow(() -> new RuntimeException("试卷不存在"));
        }

        Exam exam = new Exam();
        exam.setTeacher(teacher);
        exam.setTitle(examDTO.getTitle());
        exam.setDescription(examDTO.getDescription());
        exam.setTotalScore(examPaper != null ? examPaper.getTotalScore() : examDTO.getTotalScore());
        exam.setDuration(examPaper != null ? examPaper.getDuration() : examDTO.getDuration());
        exam.setStartTime(examDTO.getStartTime());
        exam.setEndTime(examDTO.getEndTime());
        exam.setStatus(Exam.ExamStatus.DRAFT);
        exam.setScoresPublished(false);

        if (examPaper != null) {
            exam.setExamPaper(examPaper);
            if (examPaper.getCourse() != null) {
                exam.setCourse(examPaper.getCourse());
            }
        }

        if (examDTO.getCourseId() != null) {
            Course course = courseRepository.findById(examDTO.getCourseId())
                    .orElseThrow(() -> new RuntimeException("课程不存在"));
            exam.setCourse(course);
        }

        Exam savedExam = examRepository.save(exam);
        return toExamDTO(savedExam);
    }

    @Transactional
    public ExamDTO publishExam(Long examId, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("考试不存在"));

        if (!exam.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能发布自己创建的考试");
        }

        if (exam.getExamPaper() == null && (exam.getTotalScore() == null || exam.getTotalScore() <= 0)) {
            throw new RuntimeException("考试缺少试卷或总分设置");
        }

        exam.setStatus(Exam.ExamStatus.PUBLISHED);
        exam.setPublishedAt(LocalDateTime.now());

        Exam savedExam = examRepository.save(exam);

        createSubmissionsForExam(savedExam);

        return toExamDTO(savedExam);
    }

    private void createSubmissionsForExam(Exam exam) {
        List<Enrollment> enrollments;
        if (exam.getCourse() != null) {
            enrollments = enrollmentRepository.findEnrolledStudentsWithDetails(exam.getCourse().getId());
        } else if (exam.getExamPaper() != null && exam.getExamPaper().getCourse() != null) {
            enrollments = enrollmentRepository.findEnrolledStudentsWithDetails(exam.getExamPaper().getCourse().getId());
        } else {
            return;
        }

        for (Enrollment enrollment : enrollments) {
            boolean exists = submissionRepository.findByExamIdAndStudentId(
                    exam.getId(), enrollment.getStudent().getId()).isPresent();

            if (!exists) {
                ExamSubmission submission = new ExamSubmission();
                submission.setExam(exam);
                submission.setStudent(enrollment.getStudent());
                submission.setStatus(ExamSubmission.SubmissionStatus.NOT_STARTED);
                submissionRepository.save(submission);
            }
        }
    }

    @Transactional
    public ExamSubmissionDTO startExam(Long examId, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("考试不存在"));

        if (exam.getStatus() != Exam.ExamStatus.PUBLISHED && exam.getStatus() != Exam.ExamStatus.IN_PROGRESS) {
            throw new RuntimeException("该考试尚未开放或已结束");
        }

        if (exam.getStartTime() != null && LocalDateTime.now().isBefore(exam.getStartTime())) {
            throw new RuntimeException("考试尚未开始");
        }

        if (exam.getEndTime() != null && LocalDateTime.now().isAfter(exam.getEndTime())) {
            throw new RuntimeException("考试已结束");
        }

        Optional<ExamSubmission> submissionOpt = submissionRepository.findByExamIdAndStudentId(examId, student.getId());
        ExamSubmission submission;

        if (submissionOpt.isPresent()) {
            submission = submissionOpt.get();
            if (submission.getStatus() == ExamSubmission.SubmissionStatus.SUBMITTED ||
                    submission.getStatus() == ExamSubmission.SubmissionStatus.GRADED) {
                throw new RuntimeException("您已经提交过考试");
            }
            if (submission.getStatus() == ExamSubmission.SubmissionStatus.IN_PROGRESS) {
                return toExamSubmissionDTO(submission);
            }
        } else {
            submission = new ExamSubmission();
            submission.setExam(exam);
            submission.setStudent(student);
        }

        submission.setStatus(ExamSubmission.SubmissionStatus.IN_PROGRESS);
        submission.setStartedAt(LocalDateTime.now());

        ExamSubmission savedSubmission = submissionRepository.save(submission);

        initializeExamAnswers(savedSubmission, exam);

        return toExamSubmissionDTO(savedSubmission);
    }

    private void initializeExamAnswers(ExamSubmission submission, Exam exam) {
        List<ExamPaperQuestion> questions;
        if (exam.getExamPaper() != null) {
            questions = examPaperQuestionRepository.findByExamPaperId(exam.getExamPaper().getId());
        } else {
            questions = new ArrayList<>();
        }

        for (ExamPaperQuestion question : questions) {
            Optional<ExamAnswer> existingAnswer = answerRepository.findByExamSubmissionIdAndExamPaperQuestionId(
                    submission.getId(), question.getId());

            if (!existingAnswer.isPresent()) {
                ExamAnswer answer = new ExamAnswer();
                answer.setExamSubmission(submission);
                answer.setExamPaperQuestion(question);
                answer.setIsAnswered(false);
                answer.setIsAutoGraded(false);
                answerRepository.save(answer);
            }
        }
    }

    @Transactional
    public ExamAnswerDTO saveAnswer(Long submissionId, Long questionId, String studentAnswer, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        ExamSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("考试提交记录不存在"));

        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("无权操作此考试");
        }

        if (submission.getStatus() != ExamSubmission.SubmissionStatus.IN_PROGRESS) {
            throw new RuntimeException("考试已提交或已结束");
        }

        ExamAnswer answer = answerRepository.findByExamSubmissionIdAndExamPaperQuestionId(submissionId, questionId)
                .orElseThrow(() -> new RuntimeException("答案记录不存在"));

        answer.setStudentAnswer(studentAnswer);
        answer.setIsAnswered(studentAnswer != null && !studentAnswer.trim().isEmpty());
        answer.setAnsweredAt(LocalDateTime.now());

        ExamPaperQuestion paperQuestion = answer.getExamPaperQuestion();
        Question question = paperQuestion.getQuestion();

        if (isObjectiveQuestion(question.getType())) {
            boolean isCorrect = checkAnswer(question, studentAnswer);
            answer.setIsAutoGraded(true);
            answer.setIsCorrect(isCorrect);
            answer.setScore(isCorrect ? paperQuestion.getScore() : 0.0);
        }

        ExamAnswer savedAnswer = answerRepository.save(answer);
        return toExamAnswerDTO(savedAnswer);
    }

    private boolean isObjectiveQuestion(Question.QuestionType type) {
        return type == Question.QuestionType.SINGLE_CHOICE ||
               type == Question.QuestionType.MULTIPLE_CHOICE ||
               type == Question.QuestionType.TRUE_FALSE;
    }

    private boolean checkAnswer(Question question, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isEmpty()) {
            return false;
        }

        String correctAnswer = question.getAnswer().trim();
        String studentAns = studentAnswer.trim();

        if (question.getType() == Question.QuestionType.MULTIPLE_CHOICE) {
            String sortedCorrect = sortString(correctAnswer.toUpperCase());
            String sortedStudent = sortString(studentAns.toUpperCase());
            return sortedCorrect.equals(sortedStudent);
        }

        return correctAnswer.equalsIgnoreCase(studentAns);
    }

    private String sortString(String s) {
        char[] chars = s.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

    @Transactional
    public ExamSubmissionDTO submitExam(Long submissionId, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        ExamSubmission submission = submissionRepository.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("考试提交记录不存在"));

        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("无权操作此考试");
        }

        if (submission.getStatus() == ExamSubmission.SubmissionStatus.SUBMITTED ||
                submission.getStatus() == ExamSubmission.SubmissionStatus.GRADED) {
            throw new RuntimeException("考试已提交");
        }

        submission.setStatus(ExamSubmission.SubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(LocalDateTime.now());

        double objectiveScore = 0.0;
        if (submission.getAnswers() != null) {
            for (ExamAnswer answer : submission.getAnswers()) {
                if (answer.getIsAutoGraded() && answer.getScore() != null) {
                    objectiveScore += answer.getScore();
                }
            }
        }
        submission.setObjectiveScore(objectiveScore);

        ExamSubmission savedSubmission = submissionRepository.save(submission);
        return toExamSubmissionDTO(savedSubmission);
    }

    public List<ExamSubmissionDTO> getExamSubmissions(Long examId) {
        List<ExamSubmission> submissions = submissionRepository.findByExamIdWithAnswers(examId);
        return submissions.stream().map(this::toExamSubmissionDTO).collect(Collectors.toList());
    }

    public ExamSubmissionDTO getSubmissionForGrading(Long submissionId) {
        ExamSubmission submission = submissionRepository.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));
        return toExamSubmissionDTO(submission);
    }

    @Transactional
    public ExamAnswerDTO gradeSubjectiveAnswer(Long answerId, Double score, String teacherComment, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        ExamAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("答案记录不存在"));

        ExamSubmission submission = answer.getExamSubmission();
        Exam exam = submission.getExam();

        if (!exam.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能评分自己课程的考试");
        }

        if (answer.getIsAutoGraded()) {
            throw new RuntimeException("客观题已自动评分，不可修改");
        }

        answer.setScore(score);
        answer.setTeacherComment(teacherComment);
        answer.setIsAutoGraded(false);

        ExamAnswer savedAnswer = answerRepository.save(answer);
        return toExamAnswerDTO(savedAnswer);
    }

    @Transactional
    public ExamSubmissionDTO completeGrading(Long submissionId, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        ExamSubmission submission = submissionRepository.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("提交记录不存在"));

        Exam exam = submission.getExam();
        if (!exam.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能评分自己课程的考试");
        }

        double totalScore = 0.0;
        double subjectiveScore = 0.0;
        boolean allGraded = true;

        if (submission.getAnswers() != null) {
            for (ExamAnswer answer : submission.getAnswers()) {
                if (answer.getScore() != null) {
                    totalScore += answer.getScore();
                    if (!answer.getIsAutoGraded()) {
                        subjectiveScore += answer.getScore();
                    }
                } else if (!answer.getIsAutoGraded()) {
                    allGraded = false;
                }
            }
        }

        if (!allGraded) {
            throw new RuntimeException("存在未评分的主观题");
        }

        submission.setTotalScore(totalScore);
        submission.setSubjectiveScore(subjectiveScore);
        submission.setStatus(ExamSubmission.SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());

        ExamSubmission savedSubmission = submissionRepository.save(submission);
        return toExamSubmissionDTO(savedSubmission);
    }

    @Transactional
    public ExamDTO publishScores(Long examId, String teacherUsername) {
        User teacher = userRepository.findByUsername(teacherUsername)
                .orElseThrow(() -> new RuntimeException("教师不存在"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("考试不存在"));

        if (!exam.getTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("只能操作自己创建的考试");
        }

        List<ExamSubmission> submissions = submissionRepository.findByExamId(examId);
        for (ExamSubmission submission : submissions) {
            if (submission.getStatus() == ExamSubmission.SubmissionStatus.SUBMITTED) {
                throw new RuntimeException("存在未评分的提交：" + submission.getStudent().getName());
            }
        }

        exam.setScoresPublished(true);
        exam.setStatus(Exam.ExamStatus.ENDED);
        Exam savedExam = examRepository.save(exam);

        return toExamDTO(savedExam);
    }

    public List<ExamDTO> getStudentExams(String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        List<Enrollment> enrollments = enrollmentRepository.findEnrolledCoursesWithDetails(student.getId());
        List<Long> courseIds = enrollments.stream()
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toList());

        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Exam.ExamStatus> statuses = Arrays.asList(
                Exam.ExamStatus.PUBLISHED,
                Exam.ExamStatus.IN_PROGRESS,
                Exam.ExamStatus.ENDED
        );

        List<Exam> exams = examRepository.findByCourseIdsAndStatuses(courseIds, statuses);
        List<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toList());

        List<ExamSubmission> submissions = submissionRepository.findByExamIdsAndStudentId(examIds, student.getId());
        Map<Long, ExamSubmission> submissionMap = submissions.stream()
                .collect(Collectors.toMap(s -> s.getExam().getId(), s -> s));

        return exams.stream()
                .map(e -> toExamDTOWithSubmission(e, submissionMap.get(e.getId())))
                .collect(Collectors.toList());
    }

    public ExamSubmissionDTO getMySubmission(Long examId, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        Optional<ExamSubmission> submissionOpt = submissionRepository.findByExamIdAndStudentId(examId, student.getId());
        return submissionOpt.map(this::toExamSubmissionDTO).orElse(null);
    }

    public ExamSubmissionDTO getMyResult(Long examId, String studentUsername) {
        User student = userRepository.findByUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("学生不存在"));

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("考试不存在"));

        if (!exam.getScoresPublished()) {
            throw new RuntimeException("成绩尚未公布");
        }

        Optional<ExamSubmission> submissionOpt = submissionRepository.findByExamIdAndStudentId(examId, student.getId());
        if (!submissionOpt.isPresent()) {
            throw new RuntimeException("您没有参加此考试");
        }

        return toExamSubmissionDTO(submissionOpt.get());
    }

    private ExamDTO toExamDTO(Exam exam) {
        ExamDTO dto = new ExamDTO();
        dto.setId(exam.getId());
        dto.setTitle(exam.getTitle());
        dto.setDescription(exam.getDescription());
        dto.setTotalScore(exam.getTotalScore());
        dto.setDuration(exam.getDuration());
        dto.setStartTime(exam.getStartTime());
        dto.setEndTime(exam.getEndTime());
        dto.setStatus(exam.getStatus());
        dto.setStatusName(exam.getStatus() != null ? exam.getStatus().getDescription() : null);
        dto.setScoresPublished(exam.getScoresPublished());
        dto.setPublishedAt(exam.getPublishedAt());

        dto.setTeacherId(exam.getTeacher().getId());
        dto.setTeacherName(exam.getTeacher().getName());

        if (exam.getExamPaper() != null) {
            dto.setExamPaperId(exam.getExamPaper().getId());
            dto.setExamPaperTitle(exam.getExamPaper().getTitle());
        }

        if (exam.getCourse() != null) {
            dto.setCourseId(exam.getCourse().getId());
            dto.setCourseName(exam.getCourse().getCourseName());
        }

        if (exam.getCreatedAt() != null) {
            dto.setCreatedAt(exam.getCreatedAt().format(formatter));
        }
        if (exam.getUpdatedAt() != null) {
            dto.setUpdatedAt(exam.getUpdatedAt().format(formatter));
        }

        if (exam.getCourse() != null) {
            List<Enrollment> enrollments = enrollmentRepository.findByCourseIdAndStatus(
                    exam.getCourse().getId(), Enrollment.EnrollmentStatus.ENROLLED);
            dto.setTotalStudents(enrollments.size());
        }

        dto.setSubmittedCount(submissionRepository.countByExamIdAndStatus(
                exam.getId(), ExamSubmission.SubmissionStatus.SUBMITTED).intValue());
        dto.setGradedCount(submissionRepository.countByExamIdAndStatus(
                exam.getId(), ExamSubmission.SubmissionStatus.GRADED).intValue());

        if (exam.getExamPaper() != null && exam.getExamPaper().getQuestions() != null) {
            dto.setQuestions(exam.getExamPaper().getQuestions().stream()
                    .sorted(Comparator.comparing(ExamPaperQuestion::getSortOrder))
                    .map(this::toExamPaperQuestionDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private ExamDTO toExamDTOWithSubmission(Exam exam, ExamSubmission submission) {
        ExamDTO dto = toExamDTO(exam);
        if (submission != null) {
            dto.setMySubmission(toExamSubmissionDTO(submission));
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

    private ExamSubmissionDTO toExamSubmissionDTO(ExamSubmission submission) {
        ExamSubmissionDTO dto = new ExamSubmissionDTO();
        dto.setId(submission.getId());
        dto.setExamId(submission.getExam().getId());
        dto.setExamTitle(submission.getExam().getTitle());
        dto.setStudentId(submission.getStudent().getId());
        dto.setStudentName(submission.getStudent().getName());
        dto.setStudentUsername(submission.getStudent().getUsername());
        dto.setStatus(submission.getStatus());
        dto.setStatusName(submission.getStatus() != null ? submission.getStatus().getDescription() : null);
        dto.setStartedAt(submission.getStartedAt());
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setGradedAt(submission.getGradedAt());
        dto.setTotalScore(submission.getTotalScore());
        dto.setObjectiveScore(submission.getObjectiveScore());
        dto.setSubjectiveScore(submission.getSubjectiveScore());
        dto.setMaxScore(submission.getExam().getTotalScore());
        dto.setTeacherComment(submission.getTeacherComment());

        if (submission.getAnswers() != null && !submission.getAnswers().isEmpty()) {
            dto.setAnswers(submission.getAnswers().stream()
                    .sorted(Comparator.comparing(a -> a.getExamPaperQuestion().getSortOrder()))
                    .map(this::toExamAnswerDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private ExamAnswerDTO toExamAnswerDTO(ExamAnswer answer) {
        ExamAnswerDTO dto = new ExamAnswerDTO();
        dto.setId(answer.getId());
        dto.setExamSubmissionId(answer.getExamSubmission().getId());
        dto.setExamPaperQuestionId(answer.getExamPaperQuestion().getId());
        dto.setStudentAnswer(answer.getStudentAnswer());
        dto.setIsAnswered(answer.getIsAnswered());
        dto.setScore(answer.getScore());
        dto.setIsAutoGraded(answer.getIsAutoGraded());
        dto.setIsCorrect(answer.getIsCorrect());
        dto.setTeacherComment(answer.getTeacherComment());
        dto.setAnsweredAt(answer.getAnsweredAt());
        dto.setMaxScore(answer.getExamPaperQuestion().getScore());

        ExamPaperQuestion pq = answer.getExamPaperQuestion();
        Question q = pq.getQuestion();
        dto.setSortOrder(pq.getSortOrder());
        dto.setType(q.getType().name());
        dto.setTypeName(q.getType().getDescription());
        dto.setContent(q.getContent());
        dto.setOptions(q.getOptions());
        dto.setCorrectAnswer(q.getAnswer());
        dto.setExplanation(q.getExplanation());

        return dto;
    }
}
