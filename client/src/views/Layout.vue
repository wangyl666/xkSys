<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <h3>教务管理系统</h3>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <template v-if="userStore.isTeacher">
          <el-sub-menu index="teacher">
            <template #title>
              <el-icon><Notebook /></el-icon>
              <span>课程管理</span>
            </template>
            <el-menu-item index="/courses">课程列表</el-menu-item>
            <el-menu-item index="/course/create">发布课程</el-menu-item>
            <el-menu-item index="/teacher-schedule">我的课表</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/attendance-management">
            <el-icon><Document /></el-icon>
            <span>考勤管理</span>
          </el-menu-item>
          <el-menu-item index="/homework-management">
            <el-icon><Edit /></el-icon>
            <span>作业管理</span>
          </el-menu-item>
        </template>

        <template v-else-if="userStore.isStudent">
          <el-sub-menu index="student">
            <template #title>
              <el-icon><Reading /></el-icon>
              <span>学习中心</span>
            </template>
            <el-menu-item index="/select-courses">选课中心</el-menu-item>
            <el-menu-item index="/my-courses">我的课程</el-menu-item>
            <el-menu-item index="/schedule">我的课表</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/student-attendance">
            <el-icon><Document /></el-icon>
            <span>我的考勤</span>
          </el-menu-item>
          <el-menu-item index="/student-homework">
            <el-icon><Edit /></el-icon>
            <span>我的作业</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">
              {{ currentTitle }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown
            v-if="userStore.isStudent"
            trigger="click"
            @visible-change="handleNotificationVisibleChange"
          >
            <span class="notification-icon">
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="item">
                <el-icon size="20"><Bell /></el-icon>
              </el-badge>
            </span>
            <template #dropdown>
              <el-dropdown-menu class="notification-dropdown">
                <div class="notification-header">
                  <span>消息通知</span>
                  <el-button type="text" size="small" @click.stop="markAllRead" v-if="unreadCount > 0">
                    全部已读
                  </el-button>
                </div>
                <el-divider style="margin: 8px 0" />
                <div class="notification-list" v-if="unreadNotifications.length > 0">
                  <div
                    v-for="notification in unreadNotifications"
                    :key="notification.id"
                    class="notification-item"
                    @click="handleNotificationClick(notification)"
                  >
                    <div class="notification-icon-wrap" :class="getNotificationTypeClass(notification.type)">
                      <el-icon :size="16">
                        <component :is="getNotificationIcon(notification.type)" />
                      </el-icon>
                    </div>
                    <div class="notification-content">
                      <div class="notification-title">{{ getNotificationTitle(notification.type) }}</div>
                      <div class="notification-message">{{ notification.message }}</div>
                      <div class="notification-time">{{ formatTime(notification.sentAt) }}</div>
                    </div>
                    <div class="notification-unread-dot" v-if="notification.status === 'UNREAD'"></div>
                  </div>
                </div>
                <el-empty v-else description="暂无新消息" :image-size="80" />
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.user?.name }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <transition name="notification-banner">
      <div v-if="showBanner" class="notification-banner" @click="closeBanner">
        <div class="banner-content">
          <div class="banner-icon" :class="bannerType">
            <el-icon :size="24">
              <component :is="getBannerIcon(bannerType)" />
            </el-icon>
          </div>
          <div class="banner-text">
            <div class="banner-title">{{ bannerTitle }}</div>
            <div class="banner-message">{{ bannerMessage }}</div>
          </div>
        </div>
        <el-icon class="banner-close" @click.stop="closeBanner"><Close /></el-icon>
      </div>
    </transition>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUnreadNotifications, getUnreadCount, markNotificationRead, markAllNotificationsRead } from '@/api/notifications'
import { Bell, Close, Warning, Clock, Document, User, Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const unreadCount = ref(0)
const unreadNotifications = ref([])
const showBanner = ref(false)
const bannerTitle = ref('')
const bannerMessage = ref('')
const bannerType = ref('warning')

let pollInterval = null
let previousUnreadIds = new Set()

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => route.meta?.title)

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
  }
}

const fetchUnreadCount = async () => {
  if (!userStore.isStudent) return
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data
  } catch (error) {
    console.error('获取未读消息数量失败:', error)
  }
}

const fetchUnreadNotifications = async () => {
  if (!userStore.isStudent) return
  try {
    const res = await getUnreadNotifications()
    const currentNotifications = res.data
    
    const currentIds = new Set(currentNotifications.map(n => n.id))
    const newNotifications = currentNotifications.filter(n => !previousUnreadIds.has(n.id))
    
    if (newNotifications.length > 0) {
      showNewNotificationBanner(newNotifications[0])
    }
    
    previousUnreadIds = currentIds
    unreadNotifications.value = currentNotifications
    unreadCount.value = currentNotifications.length
  } catch (error) {
    console.error('获取未读消息失败:', error)
  }
}

const showNewNotificationBanner = (notification) => {
  bannerTitle.value = getNotificationTitle(notification.type)
  bannerMessage.value = notification.message
  bannerType.value = getBannerType(notification.type)
  showBanner.value = true
  
  setTimeout(() => {
    closeBanner()
  }, 5000)
}

const closeBanner = () => {
  showBanner.value = false
}

const handleNotificationVisibleChange = (visible) => {
  if (visible) {
    fetchUnreadNotifications()
  }
}

const handleNotificationClick = async (notification) => {
  if (notification.status === 'UNREAD') {
    try {
      await markNotificationRead(notification.id)
      fetchUnreadCount()
      fetchUnreadNotifications()
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

const markAllRead = async () => {
  try {
    await markAllNotificationsRead()
    ElMessage.success('所有消息已标记为已读')
    fetchUnreadCount()
    fetchUnreadNotifications()
  } catch (error) {
    console.error('标记全部已读失败:', error)
    ElMessage.error('操作失败')
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

const getBannerType = (type) => {
  const map = {
    ATTENDANCE_ABSENT: 'danger',
    HOMEWORK_REMINDER: 'warning',
    HOMEWORK_GRADED: 'success',
    SYSTEM: 'info'
  }
  return map[type] || 'info'
}

const getBannerIcon = (type) => {
  const map = {
    danger: Warning,
    warning: Clock,
    success: Check,
    info: Bell
  }
  return map[type] || Bell
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

const startPolling = () => {
  if (pollInterval) {
    clearInterval(pollInterval)
  }
  pollInterval = setInterval(() => {
    if (userStore.isStudent) {
      fetchUnreadNotifications()
    }
  }, 30000)
}

onMounted(() => {
  if (userStore.isStudent) {
    fetchUnreadNotifications()
    startPolling()
  }
})

watch(
  () => userStore.isStudent,
  (isStudent) => {
    if (isStudent) {
      fetchUnreadNotifications()
      startPolling()
    } else {
      if (pollInterval) {
        clearInterval(pollInterval)
      }
    }
  }
)

onUnmounted(() => {
  if (pollInterval) {
    clearInterval(pollInterval)
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #304156;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #3a4a5c;
}

.logo h3 {
  color: white;
  font-size: 16px;
  margin: 0;
}

.header {
  background-color: white;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notification-icon {
  cursor: pointer;
  color: #606266;
  padding: 8px;
  border-radius: 4px;
  transition: all 0.2s;
}

.notification-icon:hover {
  background-color: #f5f7fa;
  color: #409eff;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
}

.notification-dropdown {
  width: 360px;
  padding: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 0;
  font-weight: 500;
  font-size: 14px;
  color: #303133;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-icon-wrap {
  width: 36px;
  height: 36px;
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

.notification-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.notification-message {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.notification-unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #f56c6c;
  flex-shrink: 0;
  margin-left: 8px;
}

.notification-banner {
  position: fixed;
  right: 20px;
  bottom: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  padding: 16px 20px;
  min-width: 320px;
  max-width: 400px;
  z-index: 9999;
  cursor: pointer;
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.banner-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
}

.banner-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.banner-icon.danger {
  background-color: #fef0f0;
  color: #f56c6c;
}

.banner-icon.warning {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.banner-icon.success {
  background-color: #f0f9eb;
  color: #67c23a;
}

.banner-icon.info {
  background-color: #ecf5ff;
  color: #409eff;
}

.banner-text {
  flex: 1;
  min-width: 0;
}

.banner-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.banner-message {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.banner-close {
  cursor: pointer;
  color: #909399;
  font-size: 16px;
  flex-shrink: 0;
  transition: color 0.2s;
}

.banner-close:hover {
  color: #606266;
}

.notification-banner-enter-active,
.notification-banner-leave-active {
  transition: all 0.3s ease;
}

.notification-banner-enter-from,
.notification-banner-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>