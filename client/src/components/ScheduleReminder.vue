<template>
  <div class="schedule-reminder">
    <el-alert
      v-for="(reminder, index) in upcomingReminders"
      :key="index"
      :title="reminder.title"
      :type="reminder.type"
      :closable="true"
      show-icon
      style="margin-bottom: 12px"
    >
      <template #default>
        <div class="reminder-details">
          <div class="reminder-time">
            <el-icon><Clock /></el-icon>
            {{ reminder.timeText }}
          </div>
          <div class="reminder-location">
            <el-icon><Location /></el-icon>
            {{ reminder.location }}
          </div>
          <div class="reminder-section">
            <el-icon><Calendar /></el-icon>
            第{{ reminder.startSection }}-{{ reminder.endSection }}节
          </div>
          <div v-if="reminder.teacherName" class="reminder-teacher">
            <el-icon><User /></el-icon>
            教师：{{ reminder.teacherName }}
          </div>
        </div>
      </template>
    </el-alert>

    <el-empty
      v-if="!loading && upcomingReminders.length === 0 && todayReminders.length === 0"
      description="今日暂无课程安排"
      :image-size="60"
    />

    <el-card v-if="todayReminders.length > 0" style="margin-top: 16px">
      <template #header>
        <div class="today-header">
          <el-icon><Document /></el-icon>
          <span>今日课程安排</span>
          <el-tag size="small" type="info">{{ todayReminders.length }}门课</el-tag>
        </div>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="(item, index) in todayReminders"
          :key="index"
          :type="item.isCurrent ? 'primary' : (item.isPast ? 'info' : 'warning')"
          :icon="item.isCurrent ? VideoPlay : (item.isPast ? CircleCheck : Clock)"
          placement="top"
        >
          <el-card :class="['course-card', { 'current': item.isCurrent, 'past': item.isPast }]">
            <div class="course-header">
              <span class="course-name">{{ item.courseName }}</span>
              <el-tag :type="item.isCurrent ? 'primary' : (item.isPast ? 'info' : 'warning')" size="small">
                {{ item.isCurrent ? '正在上课' : (item.isPast ? '已结束' : '未开始') }}
              </el-tag>
            </div>
            <div class="course-info">
              <div class="info-item">
                <el-icon><Clock /></el-icon>
                <span>{{ item.timeText }}</span>
              </div>
              <div class="info-item">
                <el-icon><Location /></el-icon>
                <span>{{ item.location }}</span>
              </div>
              <div class="info-item">
                <el-icon><Calendar /></el-icon>
                <span>第{{ item.startSection }}-{{ item.endSection }}节</span>
              </div>
              <div v-if="item.teacherName" class="info-item">
                <el-icon><User /></el-icon>
                <span>{{ item.teacherName }}</span>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getMyCourses } from '@/api/enrollments'
import { getTeacherCourses } from '@/api/courses'
import { ElMessage } from 'element-plus'

const props = defineProps({
  isTeacher: {
    type: Boolean,
    default: false
  }
})

const courses = ref([])
const loading = ref(false)
const currentTime = ref(new Date())

let timer = null

const dayMap = {
  0: 'SUNDAY',
  1: 'MONDAY',
  2: 'TUESDAY',
  3: 'WEDNESDAY',
  4: 'THURSDAY',
  5: 'FRIDAY',
  6: 'SATURDAY'
}

const sectionTimeMap = {
  1: { start: '08:00', end: '08:45' },
  2: { start: '08:55', end: '09:40' },
  3: { start: '10:00', end: '10:45' },
  4: { start: '10:55', end: '11:40' },
  5: { start: '14:00', end: '14:45' },
  6: { start: '14:55', end: '15:40' },
  7: { start: '16:00', end: '16:45' },
  8: { start: '16:55', end: '17:40' },
  9: { start: '19:00', end: '19:45' },
  10: { start: '19:55', end: '20:40' },
  11: { start: '20:50', end: '21:35' },
  12: { start: '21:45', end: '22:30' }
}

const getCurrentDayOfWeek = () => {
  const day = currentTime.value.getDay()
  return dayMap[day]
}

const getCurrentTimeStr = () => {
  const hours = currentTime.value.getHours().toString().padStart(2, '0')
  const minutes = currentTime.value.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

const isTimeInRange = (currentTime, startTime, endTime) => {
  return currentTime >= startTime && currentTime <= endTime
}

const isTimeBefore = (currentTime, targetTime) => {
  return currentTime < targetTime
}

const getMinutesDiff = (time1, time2) => {
  const [h1, m1] = time1.split(':').map(Number)
  const [h2, m2] = time2.split(':').map(Number)
  return (h1 * 60 + m1) - (h2 * 60 + m2)
}

const todayCourses = computed(() => {
  const today = getCurrentDayOfWeek()
  return courses.value.filter(course => course.dayOfWeek === today)
})

const todayReminders = computed(() => {
  const currentTimeStr = getCurrentTimeStr()
  
  return todayCourses.value
    .map(course => {
      const startTime = sectionTimeMap[course.startSection].start
      const endTime = sectionTimeMap[course.endSection].end
      const timeText = `${startTime} - ${endTime}`
      
      let isCurrent = false
      let isPast = false
      
      if (isTimeInRange(currentTimeStr, startTime, endTime)) {
        isCurrent = true
      } else if (isTimeBefore(currentTimeStr, startTime)) {
        isPast = false
      } else {
        isPast = true
      }
      
      return {
        ...course,
        startTime,
        endTime,
        timeText,
        isCurrent,
        isPast
      }
    })
    .sort((a, b) => a.startSection - b.startSection)
})

const upcomingReminders = computed(() => {
  const currentTimeStr = getCurrentTimeStr()
  const reminders = []
  
  todayReminders.value.forEach(course => {
    if (course.isCurrent) {
      reminders.push({
        title: `正在上课：${course.courseName}`,
        type: 'primary',
        timeText: course.timeText,
        location: course.location || '待定',
        startSection: course.startSection,
        endSection: course.endSection,
        teacherName: course.teacherName
      })
    } else if (!course.isPast) {
      const minutesUntilStart = getMinutesDiff(course.startTime, currentTimeStr)
      if (minutesUntilStart <= 60 && minutesUntilStart >= 0) {
        const minsText = minutesUntilStart === 0 ? '马上' : `${minutesUntilStart}分钟后`
        reminders.push({
          title: `课程提醒：${course.courseName} 将在${minsText}开始`,
          type: 'warning',
          timeText: course.timeText,
          location: course.location || '待定',
          startSection: course.startSection,
          endSection: course.endSection,
          teacherName: course.teacherName
        })
      }
    }
  })
  
  return reminders
})

const fetchCourses = async () => {
  loading.value = true
  try {
    let res
    if (props.isTeacher) {
      res = await getTeacherCourses()
    } else {
      res = await getMyCourses()
    }
    courses.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const updateTime = () => {
  currentTime.value = new Date()
}

onMounted(() => {
  fetchCourses()
  timer = setInterval(updateTime, 60000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>

<style scoped>
.schedule-reminder {
}

.reminder-details {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 8px;
  font-size: 13px;
}

.reminder-details > div {
  display: flex;
  align-items: center;
  gap: 4px;
}

.today-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.today-header span {
  font-weight: 500;
}

.course-card {
  transition: all 0.3s;
}

.course-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.course-card.current {
  border-color: #409eff;
}

.course-card.current .course-name {
  color: #409eff;
}

.course-card.past {
  opacity: 0.6;
}

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.course-name {
  font-weight: 500;
  font-size: 15px;
}

.course-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}

:deep(.el-timeline-item__icon) {
  border: none;
}

:deep(.el-timeline-item__tail) {
  border-left-style: dashed;
}
</style>
