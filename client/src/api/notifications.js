import request from '@/utils/request'

export function getNotifications() {
  return request.get('/notifications')
}

export function getUnreadNotifications() {
  return request.get('/notifications/unread')
}

export function getUnreadCount() {
  return request.get('/notifications/unread-count')
}

export function markNotificationRead(id) {
  return request.put(`/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return request.put('/notifications/read-all')
}
