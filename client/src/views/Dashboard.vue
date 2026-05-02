<template>
  <div class="dashboard">
    <el-card class="welcome-card">
      <h2>欢迎使用学校教务管理系统</h2>
      <p v-if="userStore.isTeacher">尊敬的{{ userStore.user?.name }}老师，欢迎您！</p>
      <p v-else-if="userStore.isStudent">{{ userStore.user?.name }}同学，欢迎您！</p>
    </el-card>

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
import { useUserStore } from '@/stores/user'
import { getTeacherCourses } from '@/api/courses'
import { getMyCourses } from '@/api/enrollments'

const userStore = useUserStore()

const myCourses = ref([])
const enrolledCourses = ref([])

const totalCredits = computed(() => {
  return enrolledCourses.value.reduce((sum, course) => sum + course.credits, 0)
})

onMounted(async () => {
  if (userStore.isTeacher) {
    const res = await getTeacherCourses()
    myCourses.value = res.data
  } else if (userStore.isStudent) {
    const res = await getMyCourses()
    enrolledCourses.value = res.data
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

.stats-row {
  margin-bottom: 20px;
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