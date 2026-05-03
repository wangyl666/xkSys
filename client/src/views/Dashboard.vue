<template>
  <div class="dashboard">
    <el-card class="welcome-card">
      <h2>欢迎使用学校教务管理系统</h2>
      <p v-if="userStore.isTeacher">尊敬的{{ userStore.user?.name }}老师，欢迎您！</p>
      <p v-else-if="userStore.isStudent">{{ userStore.user?.name }}同学，欢迎您！</p>
    </el-card>

    <el-card v-if="userStore.isStudent && notifications.length > 0" class="notifications-card">
      <template #header>
        <div class="notifications-header">
          <el-icon><Bell /></el-icon>
          <span>消息提醒</span>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="item" />
          <el-button type="text" size="small" @click="goToNotifications" v-if="notifications.length > 3">
            查看全部
          </el-button>
        </div>
      </template>
      <div class="notifications-list">
        <div
          v-for="notification in displayedNotifications"
          :key="notification.id"
          class="notification-item"
          :class="{ unread: notification.status === 'UNREAD' }"
          @click="handleNotificationClick(notification)"
        >
          <div class="notification-icon-wrap" :class="getNotificationTypeClass(notification.type)">
            <el-icon :size="20">
              <component :is="getNotificationIcon(notification.type)" />
            </el-icon>
          </div>
          <div class="notification-content">
            <div class="notification-header-row">
              <span class="notification-title">{{ getNotificationTitle(notification.type) }}</span>
              <span class="notification-time">{{ formatTime(notification.sentAt) }}</span>
            </div>
            <div class="notification-message">{{ notification.message }}</div>
          </div>
          <div class="notification-unread-dot" v-if="notification.status === 'UNREAD'"></div>
        </div>
      </div>
    </el-card>

    <ScheduleReminder :is-teacher="userStore.isTeacher" />

    <el-row :gutter="20" class="stats-row">
      <el-col :span="8" v-if="userStore.isTeacher">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon teacher">
              <el-icon size="32"><Notebook /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ myCourses.length }}</div>
              <div class="stat-label">发布的课程</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8" v-if="userStore.isStudent">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon student">
              <el-icon size="32"><Reading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ enrolledCourses.length }}</div>
              <div class="stat-label">已选课程</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8" v-if="userStore.isStudent">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon credits">
              <el-icon size="32"><Medal /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-number">{{ totalCredits }}</div>
              <div class="stat-label">已选学分</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="quick-actions">
      <template #header>
        <span>快捷操作</span>
      </template>
      <div class="actions">
        <el-button
          v-if="userStore.isTeacher"
          type="primary"
          size="large"
          @click="$router.push('/course/create')"
        >
          <el-icon><Plus /></el-icon>
          发布新课程
        </el-button>
        <el-button
          v-if="userStore.isTeacher"
          size="large"
          @click="$router.push('/courses')"
        >
          <el-icon><List /></el-icon>
          查看我的课程
        </el-button>
        <el-button
          v-if="userStore.isTeacher"
          size="large"
          @click="$router.push('/teacher-schedule')"
        >
          <el-icon><Calendar /></el-icon>
          查看我的课表
        </el-button>
        <el-button
          v-if="userStore.isStudent"
          type="primary"
          size="large"
          @click="$router.push('/select-courses')"
        >
          <el-icon><Plus /></el-icon>
          去选课
        </el-button>
        <el-button
          v-if="userStore.isStudent"
          size="large"
          @click="$router.push('/schedule')"
        >
          <el-icon><Calendar /></el-icon>
          查看课表
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getTeacherCourses } from '@/api/courses'
import { getMyCourses } from '@/api/enrollments'
import { getNotifications, markNotificationRead } from '@/api/notifications'
import ScheduleReminder from '@/components/ScheduleReminder.vue'
import { Bell, Warning, Clock, Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const myCourses = ref([])
const enrolledCourses = ref([])
const notifications = ref([])
const unreadCount = ref(0)

const totalCredits = computed(() => {
  return enrolledCourses.value.reduce((sum, course) => sum + course.credits, 0)
})

const displayedNotifications = computed(() => {
  return notifications.value.slice(0, 5)
})

const fetchNotifications = async () => {
  if (!userStore.isStudent) return
  try {
    const res = await getNotifications()
    notifications.value = res.data
    unreadCount.value = res.data.filter(n => n.status === 'UNREAD').length
  } catch (error) {
    console.error('获取通知失败:', error)
  }
}

const getNotificationTypeClass = (type) => {
  const map = {
    ATTENDANCE_ABSENT: 'attendance',
    HOMEWORK_REMINDER: 'homework',
    HOMEWORK_GRADED: 'graded',
    SYSTEM: 'system'
  }
  return map[type] || 'system'
}

const getNotificationIcon = (type) => {
  const map = {
    ATTENDANCE_ABSENT: Warning,
    HOMEWORK_REMINDER: Clock,
    HOMEWORK_GRADED: Check,
    SYSTEM: Bell
  }
  return map[type] || Bell
}

const getNotificationTitle = (type) => {
  const map = {
    ATTENDANCE_ABSENT: '考勤提醒',
    HOMEWORK_REMINDER: '作业提醒',
    HOMEWORK_GRADED: '作业批改',
    SYSTEM: '系统通知'
  }
  return map[type] || '通知'
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString()
}

const handleNotificationClick = async (notification) => {
  if (notification.status === 'UNREAD') {
    try {
      await markNotificationRead(notification.id)
      fetchNotifications()
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
  
  if (notification.type === 'HOMEWORK_REMINDER') {
    router.push('/student-homework')
  } else if (notification.type === 'ATTENDANCE_ABSENT') {
    router.push('/student-attendance')
  }
}

const goToNotifications = () => {
  ElMessage.info('请点击右上角铃铛图标查看全部消息')
}

onMounted(async () => {
  if (userStore.isTeacher) {
    const res = await getTeacherCourses()
    myCourses.value = res.data
  } else if (userStore.isStudent) {
    const res = await getMyCourses()
    enrolledCourses.value = res.data
    fetchNotifications()
  }
})
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.welcome-card {
  margin-bottom: 20px;
  text-align: center;
  padding: 30px;
}

.welcome-card h2 {
  margin-bottom: 15px;
  color: #303133;
}

.welcome-card p {
  color: #606266;
  font-size: 16px;
}

.notifications-card {
  margin-bottom: 20px;
}

.notifications-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #303133;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #fafafa;
  border-left: 3px solid #409eff;
}

.notification-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.notification-icon-wrap.attendance {
  background-color: #fef0f0;
  color: #f56c6c;
}

.notification-icon-wrap.homework {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.notification-icon-wrap.graded {
  background-color: #f0f9eb;
  color: #67c23a;
}

.notification-icon-wrap.system {
  background-color: #ecf5ff;
  color: #409eff;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.notification-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-message {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.notification-unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #409eff;
  flex-shrink: 0;
  margin-left: 8px;
}

.stats-row {
  margin-bottom: 20px;
  margin-top: 20px;
}

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon.teacher {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.student {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.credits {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-number {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.quick-actions .actions {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.quick-actions .actions button {
  padding: 12px 24px;
}
</style>
