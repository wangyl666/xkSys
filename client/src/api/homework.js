import request from '@/utils/request'

export function createHomework(data) {
  return request.post('/homeworks', data)
}

export function updateHomework(id, data) {
  return request.put(`/homeworks/${id}`, data)
}

export function publishHomework(id) {
  return request.put(`/homeworks/${id}/publish`)
}

export function getTeacherHomeworks() {
  return request.get('/homeworks/teacher')
}

export function getCourseHomeworks(courseId) {
  return request.get(`/homeworks/course/${courseId}`)
}

export function getHomeworkById(id) {
  return request.get(`/homeworks/${id}`)
}

export function getHomeworkSubmissions(homeworkId) {
  return request.get(`/homeworks/${homeworkId}/submissions`)
}

export function submitHomework(homeworkId, data) {
  return request.post(`/homeworks/${homeworkId}/submit`, data)
}

export function gradeHomework(submissionId, data) {
  return request.put(`/homeworks/submissions/${submissionId}/grade`, data)
}

export function getStudentHomeworks() {
  return request.get('/homeworks/student')
}

export function getMySubmission(homeworkId) {
  return request.get(`/homeworks/${homeworkId}/my-submission`)
}
