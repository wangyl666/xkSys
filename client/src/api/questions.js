import request from '@/utils/request'

export function getTeacherQuestions(courseId) {
  const params = courseId ? { courseId } : {}
  return request.get('/questions', { params })
}

export function getPublicQuestions() {
  return request.get('/questions/public')
}

export function getQuestionById(id) {
  return request.get(`/questions/${id}`)
}

export function createQuestion(data) {
  return request.post('/questions', data)
}

export function batchCreateQuestions(data) {
  return request.post('/questions/batch', data)
}

export function updateQuestion(id, data) {
  return request.put(`/questions/${id}`, data)
}

export function deleteQuestion(id) {
  return request.delete(`/questions/${id}`)
}

export function getQuestionTypes() {
  return request.get('/questions/types')
}

export function importQuestions(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/questions/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
