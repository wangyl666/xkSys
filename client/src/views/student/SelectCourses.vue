<template>
  <div class="select-courses">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选课中心</span>
          <el-button type="primary" @click="fetchAllCourses">
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
        <el-table-column label="剩余名额" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enrolledStudents >= row.maxStudents ? 'danger' : 'success'">
              {{ row.maxStudents - row.enrolledStudents }}/{{ row.maxStudents }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :loading="enrollingIds.includes(row.id)"
              :disabled="isEnrolled(row.id) || row.status === 'CLOSED' || row.enrolledStudents >= row.maxStudents"
              @click="handleEnroll(row)"
            >
              {{ isEnrolled(row.id) ? '已选' : '选课' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && courses.length === 0" description="暂无可选课程" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCourses } from '@/api/courses'
import { getMyCourses, enrollCourse } from '@/api/enrollments'

const courses = ref([])
const enrolledCourseIds = ref([])
const loading = ref(false)
const enrollingIds = ref([])

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

const isEnrolled = (courseId) => {
  return enrolledCourseIds.value.includes(courseId)
}

const fetchAllCourses = async () => {
  loading.value = true
  try {
    const [coursesRes, enrolledRes] = await Promise.all([
      getCourses(),
      getMyCourses()
    ])
    courses.value = coursesRes.data
    enrolledCourseIds.value = enrolledRes.data.map(c => c.id)
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleEnroll = async (course) => {
  try {
    await ElMessageBox.confirm(
      `确定要选择课程"${course.courseName}"吗？\n上课时间：${formatDayOfWeek(course.dayOfWeek)} 第${course.startSection}-${course.endSection}节`,
      '选课确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    enrollingIds.value.push(course.id)
    await enrollCourse(course.id)
    ElMessage.success('选课成功！')
    fetchAllCourses()
  } catch (error) {
    if (error !== 'cancel') {
      const message = error.response?.data?.message || '选课失败'
      ElMessage.error(message)
    }
  } finally {
    const index = enrollingIds.value.indexOf(course.id)
    if (index > -1) {
      enrollingIds.value.splice(index, 1)
    }
  }
}

onMounted(() => {
  fetchAllCourses()
})
</script>

<style scoped>
.select-courses {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>