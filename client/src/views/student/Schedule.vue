<template>
  <div class="schedule">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的课表</span>
          <el-button type="primary" @click="fetchMyCourses">
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
                    >
                      <div class="course-name">{{ getCourseAt(section, dayIndex)?.courseName }}</div>
                      <div class="course-teacher">{{ getCourseAt(section, dayIndex)?.teacherName }}</div>
                      <div class="course-location">{{ getCourseAt(section, dayIndex)?.location }}</div>
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
        description="暂无课程，快去选课吧"
        style="margin-top: 40px"
      >
        <el-button type="primary" @click="$router.push('/select-courses')">去选课</el-button>
      </el-empty>
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
        >
          <span class="legend-color" :style="{ backgroundColor: getCourseColor(course.id) }"></span>
          <span class="legend-name">{{ course.courseName }}</span>
          <span class="legend-detail">
            {{ formatDayOfWeek(course.dayOfWeek) }} 第{{ course.startSection }}-{{ course.endSection }}节 @ {{ course.location || '待定' }}
          </span>
          <span class="legend-teacher">{{ course.teacherName }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyCourses } from '@/api/enrollments'

const courses = ref([])
const loading = ref(false)

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

onMounted(() => {
  fetchMyCourses()
})
</script>

<style scoped>
.schedule {
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
  padding: 8px;
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
  margin-bottom: 4px;
  word-break: break-all;
}

.course-teacher {
  font-size: 12px;
  opacity: 0.9;
  margin-bottom: 2px;
}

.course-location {
  font-size: 12px;
  opacity: 0.8;
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

.legend-teacher {
  color: #409eff;
  font-size: 12px;
}
</style>
