import request from '@/utils/request'

export function getTeacherExamPapers(courseId) {
  const params = courseId ? { courseId } : {}
  return request.get('/exam-papers', { params })
}

export function getExamPaperById(id) {
  return request.get(`/exam-papers/${id}`)
}

export function createExamPaper(data) {
  return request.post('/exam-papers', data)
}

export function autoGenerateExamPaper(data) {
  return request.post('/exam-papers/auto-generate', data)
}

export function updateExamPaper(id, data) {
  return request.put(`/exam-papers/${id}`, data)
}

export function deleteExamPaper(id) {
  return request.delete(`/exam-papers/${id}`)
}

export function publishExamPaper(id) {
  return request.post(`/exam-papers/${id}/publish`)
}

export function getExamPaperStatuses() {
  return request.get('/exam-papers/statuses')
}

export function getCreationMethods() {
  return request.get('/exam-papers/creation-methods')
}
