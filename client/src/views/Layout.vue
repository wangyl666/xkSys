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
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => route.meta?.title)

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
  }
}
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
</style>