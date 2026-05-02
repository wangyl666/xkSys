<template>
  <div class="course-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的课程</span>
          <el-button type="primary" @click="$router.push('/course/create')">
            <el-icon><Plus /></el-icon>
            发布新课程
          </el-button>
        </div>
      </template>

      <el-table :data="courses" v-loading="loading" stripe>
        <el-table-column prop="courseName" label="课程名称" width="180" />
        <el-table-column prop="description" label="课程描述" show-overflow-tooltip />
        <el-table-column prop="credits" label="学分" width="80" />
        <el-table-column label="上课时间" width="180">
          <template #default="{ row }">
            {{ formatDayOfWeek(row.dayOfWeek) }} 第{{ row.startSection }}-{{ row.endSection }}节
          </template>
        </el-table-column>
        <el-table-column prop="location" label="上课地点" width="120" />
        <el-table-column label="选课情况" width="120">
          <template #default="{ row }">
            {{ row.enrolledStudents }}/{{ row.maxStudents }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && courses.length === 0" description="暂无课程" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherCourses, deleteCourse } from '@/api/courses'

const router = useRouter()
const courses = ref([])
const loading = ref(false)

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

const formatStatus = (status) => {
  const map = {
    AVAILABLE: '可选',
    FULL: '已满',
    CLOSED: '已关闭'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = {
    AVAILABLE: 'success',
    FULL: 'warning',
    CLOSED: 'danger'
  }
  return map[status] || 'info'
}

const fetchCourses = async () => {
  loading.value = true
  try {
    const res = await getTeacherCourses()
    courses.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleEdit = (row) => {
  router.push(`/course/edit/${row.id}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除课程"${row.courseName}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCourse(row.id)
    ElMessage.success('删除成功')
    fetchCourses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

onMounted(() => {
  fetchCourses()
})
</script>

<style scoped>
.course-list {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>