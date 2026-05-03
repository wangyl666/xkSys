<template>
  <div class="my-courses">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的课程</span>
          <el-button type="primary" @click="fetchMyCourses">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table :data="courses" v-loading="loading" stripe>
        <el-table-column prop="courseName" label="课程名称" width="180" />
        <el-table-column prop="teacherName" label="授课教师" width="120" />
        <el-table-column prop="description" label="课程描述" show-overflow-tooltip />
        <el-table-column prop="credits" label="学分" width="80" />
        <el-table-column label="上课时间" width="180">
          <template #default="{ row }">
            {{ formatDayOfWeek(row.dayOfWeek) }} 第{{ row.startSection }}-{{ row.endSection }}节
          </template>
        </el-table-column>
        <el-table-column prop="location" label="上课地点" width="120" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              title="确定要退选这门课程吗？"
              @confirm="handleDrop(row)"
            >
              <template #reference>
                <el-button type="danger" size="small" link>退选</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && courses.length === 0" description="暂无已选课程">
        <el-button type="primary" @click="$router.push('/select-courses')">去选课</el-button>
      </el-empty>
    </el-card>

    <el-card v-if="courses.length > 0" style="margin-top: 20px">
      <template #header>
        <span>选课统计</span>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="已选课程数">{{ courses.length }} 门</el-descriptions-item>
        <el-descriptions-item label="总学分">{{ totalCredits }} 学分</el-descriptions-item>
        <el-descriptions-item label="操作">
          <el-button type="primary" link @click="$router.push('/schedule')">
            查看课表
          </el-button>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyCourses, dropCourse } from '@/api/enrollments'

const courses = ref([])
const loading = ref(false)

const totalCredits = computed(() => {
  return courses.value.reduce((sum, course) => sum + course.credits, 0)
})

const formatDayOfWeek = (day) => {
  const map = {
    MONDAY: '周一',
    TUESDAY: '周二',
    WEDNESDAY: '周三',
    THURSDAY: '周四',
    FRIDAY: '周五',
    SATURDAY: '周六',
    SUNDAY: '周日'
  }
  return map[day] || day
}

const fetchMyCourses = async () => {
  loading.value = true
  try {
    const res = await getMyCourses()
    courses.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleDrop = async (course) => {
  try {
    await dropCourse(course.id)
    ElMessage.success('退选成功')
    fetchMyCourses()
  } catch (error) {
    const message = error.response?.data?.message || '退选失败'
    ElMessage.error(message)
  }
}

onMounted(() => {
  fetchMyCourses()
})
</script>

<style scoped>
.my-courses {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>