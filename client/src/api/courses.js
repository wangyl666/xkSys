import request from '@/utils/request'

export function getCourses() {
  return request.get('/courses')
}

export function getCourseById(id) {
  return request.get(`/courses/${id}`)
}

export function getTeacherCourses(teacherId) {
  const params = teacherId ? { teacherId } : {}
  return request.get('/courses/teacher', { params })
}

export function createCourse(data) {
  return request.post('/courses', data)
}

export function updateCourse(id, data) {
  return request.put(`/courses/${id}`, data)
}

export function deleteCourse(id) {
  return request.delete(`/courses/${id}`)
}