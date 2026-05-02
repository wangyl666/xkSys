import request from '@/utils/request'

export function getMyCourses() {
  return request.get('/enrollments/my-courses')
}

export function enrollCourse(courseId) {
  return request.post(`/enrollments/enroll/${courseId}`)
}

export function dropCourse(courseId) {
  return request.delete(`/enrollments/drop/${courseId}`)
}