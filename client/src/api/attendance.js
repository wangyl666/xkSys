import request from '@/utils/request'

export function createAttendance(data) {
  return request.post('/attendances', data)
}

export function batchCreateAttendance(data) {
  return request.post('/attendances/batch', data)
}

export function getTeacherAttendances() {
  return request.get('/attendances/teacher')
}

export function getStudentAttendances() {
  return request.get('/attendances/student')
}

export function getCourseAttendances(courseId) {
  return request.get(`/attendances/course/${courseId}`)
}

export function getCourseAttendancesByDate(courseId, date) {
  return request.get(`/attendances/course/${courseId}/date/${date}`)
}

export function updateAttendanceStatus(id, status, remark) {
  return request.put(`/attendances/${id}/status`, { status, remark })
}

export function deleteAttendance(id) {
  return request.delete(`/attendances/${id}`)
}

export function getAbsentStatistics(courseId) {
  return request.get(`/attendances/statistics/${courseId}`)
}

export function getNotifications() {
  return request.get('/attendance-notifications')
}

export function getUnreadNotifications() {
  return request.get('/attendance-notifications/unread')
}

export function getUnreadCount() {
  return request.get('/attendance-notifications/unread-count')
}

export function markNotificationRead(id) {
  return request.put(`/attendance-notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return request.put('/attendance-notifications/read-all')
}

export function createAppeal(data) {
  return request.post('/attendance-appeals', data)
}

export function getStudentAppeals() {
  return request.get('/attendance-appeals/student')
}

export function getTeacherAppeals() {
  return request.get('/attendance-appeals/teacher')
}

export function getPendingAppeals() {
  return request.get('/attendance-appeals/teacher/pending')
}

export function reviewAppeal(id, approved, teacherComment) {
  return request.put(`/attendance-appeals/${id}/review`, { approved, teacherComment })
}
