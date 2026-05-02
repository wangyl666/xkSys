<template>
  <div class="teacher-schedule">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的课表</span>
          <el-button type="primary" @click="fetchTeacherCourses">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <div class="schedule-container" v-loading="loading">
        <table class="schedule-table">
          <thead>
            <tr>
              <th class="section-header">节次</th>
              <th class="time-header">时间</th>
              <th>周一</th>
              <th>周二</th>
              <th>周三</th>
              <th>周四</th>
              <th>周五</th>
              <th>周六</th>
              <th>周日</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="section in 12" :key="section">
              <tr v-if="!isMergedSection(section)">
                <td class="section-cell">
                  <div class="section-num">第{{ section }}节</div>
                </td>
                <td class="time-cell">
                  <div class="section-time">{{ getSectionTime(section) }}</div>
                </td>
                <td
                  v-for="(day, dayIndex) in days"
                  :key="day"
                  :style="getCellStyle(section, dayIndex)"
                  :rowspan="getRowSpan(section, dayIndex)"
                >
                  <template v-if="hasCourseAt(section, dayIndex) && isStartSection(section, dayIndex)">
                    <div
                      class="course-cell"
                      :class="getCourseClass(section, dayIndex)"
                      :style="getCourseStyle(section, dayIndex)"
                      @click="showCourseStudents(getCourseAt(section, dayIndex))"
                    >
                      <div class="course-name">{{ getCourseAt(section, dayIndex)?.courseName }}</div>
                      <div class="course-info">
                        <span class="course-location">{{ getCourseAt(section, dayIndex)?.location }}</span>
                        <span class="course-students">{{ getCourseAt(section, dayIndex)?.enrolledStudents }}/{{ getCourseAt(section, dayIndex)?.maxStudents }}</span>
                      </div>
                      <div class="course-hint">点击查看学生</div>
                    </div>
                  </template>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <el-empty
        v-if="!loading && courses.length === 0"
        description="暂无课程"
        style="margin-top: 40px"
      />
    </el-card>

    <el-card v-if="courses.length > 0" style="margin-top: 20px">
      <template #header>
        <span>课程图例</span>
      </template>
      <div class="legend">
        <div
          v-for="course in courses"
          :key="course.id"
          class="legend-item"
          @click="showCourseStudents(course)"
        >
          <span class="legend-color" :style="{ backgroundColor: getCourseColor(course.id) }"></span>
          <span class="legend-name">{{ course.courseName }}</span>
          <span class="legend-detail">
            {{ formatDayOfWeek(course.dayOfWeek) }} 第{{ course.startSection }}-{{ course.endSection }}节 @ {{ course.location || '待定' }}
          </span>
          <span class="legend-students">{{ course.enrolledStudents }}/{{ course.maxStudents }}人</span>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="studentsDialogVisible"
      :title="`${currentCourse?.courseName} - 选课学生列表`"
      width="600px"
    >
      <el-table :data="enrolledStudents" v-loading="studentsLoading" stripe>
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="username" label="学号" width="150" />
        <el-table-column prop="department" label="院系" />
        <el-table-column prop="email" label="邮箱" />
      </el-table>
      <el-empty v-if="!studentsLoading && enrolledStudents.length === 0" description="暂无学生选课" />
      <template #footer>
        <el-button @click="studentsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeacherCourses } from '@/api/courses'
import { getEnrolledStudents } from '@/api/enrollments'

const courses = ref([])
const loading = ref(false)
const studentsDialogVisible = ref(false)
const currentCourse = ref(null)
const enrolledStudents = ref([])
const studentsLoading = ref(false)

const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

const courseColors = [
  '#667eea',
  '#f093fb',
  '#4facfe',
  '#43e97b',
  '#fa709a',
  '#fee140',
  '#30cfd0',
  '#330867',
  '#ff9a9e',
  '#a18cd1'
]

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

const getSectionTime = (section) => {
  const timeMap = {
    1: '08:00-08:45',
    2: '08:55-09:40',
    3: '10:00-10:45',
    4: '10:55-11:40',
    5: '14:00-14:45',
    6: '14:55-15:40',
    7: '16:00-16:45',
    8: '16:55-17:40',
    9: '19:00-19:45',
    10: '19:55-20:40',
    11: '20:50-21:35',
    12: '21:45-22:30'
  }
  return timeMap[section] || ''
}

const getCourseColor = (courseId) => {
  const index = courses.value.findIndex(c => c.id === courseId)
  return courseColors[index % courseColors.length]
}

const getCourseAt = (section, dayIndex) => {
  const day = days[dayIndex]
  return courses.value.find(c => {
    return c.dayOfWeek === day && section >= c.startSection && section <= c.endSection
  })
}

const hasCourseAt = (section, dayIndex) => {
  const day = days[dayIndex]
  return courses.value.some(c => {
    return c.dayOfWeek === day && section >= c.startSection && section <= c.endSection
  })
}

const isStartSection = (section, dayIndex) => {
  const course = getCourseAt(section, dayIndex)
  return course && section === course.startSection
}

const isMergedSection = (section) => {
  const course = courses.value.find(c => {
    return section > c.startSection && section <= c.endSection
  })
  return !!course
}

const getRowSpan = (section, dayIndex) => {
  const course = getCourseAt(section, dayIndex)
  if (course && section === course.startSection) {
    return course.endSection - course.startSection + 1
  }
  if (isMergedSection(section) && hasCourseAt(section, dayIndex)) {
    return 0
  }
  return 1
}

const getCellStyle = (section, dayIndex) => {
  const course = getCourseAt(section, dayIndex)
  if (course && section === course.startSection) {
    const rowSpan = course.endSection - course.startSection + 1
    return {
      verticalAlign: 'top',
      height: `${rowSpan * 60}px`
    }
  }
  return {}
}

const getCourseClass = (section, dayIndex) => {
  const course = getCourseAt(section, dayIndex)
  if (course && section === course.startSection) {
    const rowSpan = course.endSection - course.startSection + 1
    return `multi-row-${rowSpan}`
  }
  return ''
}

const getCourseStyle = (section, dayIndex) => {
  const course = getCourseAt(section, dayIndex)
  if (course && section === course.startSection) {
    const rowSpan = course.endSection - course.startSection + 1
    return {
      backgroundColor: getCourseColor(course.id),
      height: `${rowSpan * 60 - 10}px`
    }
  }
  return {}
}

const fetchTeacherCourses = async () => {
  loading.value = true
  try {
    const res = await getTeacherCourses()
    courses.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取课表失败')
  } finally {
    loading.value = false
  }
}

const showCourseStudents = async (course) => {
  if (!course) return
  currentCourse.value = course
  studentsDialogVisible.value = true
  studentsLoading.value = true
  try {
    const res = await getEnrolledStudents(course.id)
    enrolledStudents.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取学生列表失败')
  } finally {
    studentsLoading.value = false
  }
}

onMounted(() => {
  fetchTeacherCourses()
})
</script>

<style scoped>
.teacher-schedule {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.schedule-container {
  overflow-x: auto;
}

.schedule-table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
  background: white;
}

.schedule-table th,
.schedule-table td {
  border: 1px solid #e4e7ed;
  text-align: center;
  vertical-align: middle;
  padding: 5px;
}

.schedule-table th {
  background-color: #f5f7fa;
  font-weight: 500;
  color: #606266;
  height: 50px;
  min-width: 100px;
}

.section-header {
  width: 80px;
}

.time-header {
  width: 100px;
}

.section-cell {
  background-color: #f5f7fa;
  width: 80px;
  height: 60px;
}

.section-num {
  font-weight: 500;
  color: #303133;
}

.time-cell {
  background-color: #f5f7fa;
  width: 100px;
  height: 60px;
}

.section-time {
  font-size: 12px;
  color: #909399;
}

.course-cell {
  background-color: #667eea;
  color: white;
  border-radius: 4px;
  padding: 6px;
  margin: 4px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
}

.course-cell:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.course-name {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 2px;
  word-break: break-all;
}

.course-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  opacity: 0.9;
  margin-bottom: 2px;
}

.course-location {
  text-align: left;
}

.course-students {
  text-align: right;
}

.course-hint {
  font-size: 11px;
  opacity: 0.7;
  text-align: center;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  padding: 4px 8px;
  border-radius: 4px;
}

.legend-item:hover {
  background-color: #f5f7fa;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 3px;
}

.legend-name {
  font-weight: 500;
  color: #303133;
}

.legend-detail {
  color: #909399;
  font-size: 12px;
}

.legend-students {
  color: #409eff;
  font-size: 12px;
}
</style>
