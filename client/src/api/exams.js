import request from '@/utils/request'

export function getTeacherExams(courseId) {
  const params = courseId ? { courseId } : {}
  return request.get('/exams/teacher', { params })
}

export function getExamById(id) {
  return request.get(`/exams/${id}`)
}

export function getExamForStudent(id) {
  return request.get(`/exams/${id}/student`)
}

export function createExam(data) {
  return request.post('/exams', data)
}

export function publishExam(id) {
  return request.post(`/exams/${id}/publish`)
}

export function getExamSubmissions(examId) {
  return request.get(`/exams/${examId}/submissions`)
}

export function getSubmissionForGrading(submissionId) {
  return request.get(`/exams/submissions/${submissionId}/grading`)
}

export function gradeSubjectiveAnswer(answerId, data) {
  return request.put(`/exams/answers/${answerId}/grade`, data)
}

export function completeGrading(submissionId) {
  return request.post(`/exams/submissions/${submissionId}/complete-grading`)
}

export function publishScores(examId) {
  return request.post(`/exams/${examId}/publish-scores`)
}

export function getStudentExams() {
  return request.get('/exams/student')
}

export function startExam(examId) {
  return request.post(`/exams/${examId}/start`)
}

export function saveAnswer(submissionId, questionId, data) {
  return request.put(`/exams/submissions/${submissionId}/answers/${questionId}`, data)
}

export function submitExam(submissionId) {
  return request.post(`/exams/submissions/${submissionId}/submit`)
}

export function getMySubmission(examId) {
  return request.get(`/exams/${examId}/my-submission`)
}

export function getMyResult(examId) {
  return request.get(`/exams/${examId}/my-result`)
}

export function getExamStatuses() {
  return request.get('/exams/statuses')
}
