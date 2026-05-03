<template>
  <div class="student-exam">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="可参加考试" name="available">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>可参加考试</span>
              <el-select v-model="filterCourseId" placeholder="选择课程" clearable style="width: 200px">
                <el-option
                  v-for="course in courses"
                  :key="course.id"
                  :label="course.courseName"
                  :value="course.id"
                />
              </el-select>
            </div>
          </template>

          <el-table :data="availableExams" v-loading="loading" stripe>
            <el-table-column prop="title" label="考试标题" min-width="200" />
            <el-table-column prop="courseName" label="所属课程" width="120" />
            <el-table-column prop="teacherName" label="任课老师" width="100" />
            <el-table-column prop="totalScore" label="总分" width="80" />
            <el-table-column prop="duration" label="时长(分钟)" width="100" />
            <el-table-column label="考试时间" width="200">
              <template #default="{ row }">
                <div v-if="row.startTime">
                  <div>{{ formatDateTime(row.startTime) }}</div>
                  <div v-if="row.endTime" style="color: #909399; font-size: 12px">至 {{ formatDateTime(row.endTime) }}</div>
                </div>
                <span v-else style="color: #909399">随时可参加</span>
              </template>
            </el-table-column>
            <el-table-column prop="statusName" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ row.statusName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="canTakeExam(row)"
                  type="primary"
                  link
                  @click="startExam(row)"
                >
                  {{ row.mySubmission?.status === 'IN_PROGRESS' ? '继续考试' : '开始考试' }}
                </el-button>
                <el-button
                  v-else-if="row.mySubmission?.status === 'SUBMITTED'"
                  type="info"
                  link
                  disabled
                >
                  已提交等待评分
                </el-button>
                <el-button
                  v-else-if="row.mySubmission?.status === 'GRADED' && row.scoresPublished"
                  type="success"
                  link
                  @click="viewResult(row)"
                >
                  查看成绩
                </el-button>
                <el-button
                  v-else-if="row.mySubmission?.status === 'GRADED'"
                  type="info"
                  link
                  disabled
                >
                  成绩未公布
                </el-button>
                <el-button
                  v-else
                  type="info"
                  link
                  disabled
                >
                  {{ isTimeExpired(row) ? '已结束' : '未开始' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && availableExams.length === 0" description="暂无可参加的考试" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="已完成考试" name="completed">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>已完成考试</span>
              <el-select v-model="filterCourseId" placeholder="选择课程" clearable style="width: 200px">
                <el-option
                  v-for="course in courses"
                  :key="course.id"
                  :label="course.courseName"
                  :value="course.id"
                />
              </el-select>
            </div>
          </template>

          <el-table :data="completedExams" v-loading="completedLoading" stripe>
            <el-table-column prop="title" label="考试标题" min-width="200" />
            <el-table-column prop="courseName" label="所属课程" width="120" />
            <el-table-column prop="teacherName" label="任课老师" width="100" />
            <el-table-column prop="totalScore" label="总分" width="80" />
            <el-table-column label="我的得分" width="100">
              <template #default="{ row }">
                <span v-if="row.mySubmission?.totalScore !== null && row.scoresPublished" style="font-weight: bold; color: #409eff">
                  {{ row.mySubmission.totalScore }}
                </span>
                <span v-else-if="row.mySubmission?.status === 'GRADED' && !row.scoresPublished" style="color: #e6a23c">
                  等待公布
                </span>
                <span v-else style="color: #909399">-</span>
              </template>
            </el-table-column>
            <el-table-column label="提交状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getSubmissionStatusType(row.mySubmission?.status)">
                  {{ getSubmissionStatusText(row.mySubmission?.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="160">
              <template #default="{ row }">
                {{ row.mySubmission?.submittedAt ? formatDateTime(row.mySubmission.submittedAt) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.scoresPublished && row.mySubmission?.status === 'GRADED'"
                  type="primary"
                  link
                  @click="viewResult(row)"
                >
                  查看成绩
                </el-button>
                <el-button
                  v-else-if="row.mySubmission?.status === 'SUBMITTED'"
                  type="info"
                  link
                  disabled
                >
                  等待评分
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!completedLoading && completedExams.length === 0" description="暂无已完成考试" />
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStudentExams, getMySubmission } from '@/api/exams'
import { getMyCourses } from '@/api/enrollments'

const router = useRouter()

const activeTab = ref('available')
const courses = ref([])
const allExams = ref([])
const loading = ref(false)
const completedLoading = ref(false)

const filterCourseId = ref(null)

const currentTime = ref(new Date())

const filteredExams = computed(() => {
  let result = allExams.value
  if (filterCourseId.value) {
    result = result.filter(e => e.courseId === filterCourseId.value)
  }
  return result
})

const availableExams = computed(() => {
  return filteredExams.value.filter(e => {
    const status = e.status
    const myStatus = e.mySubmission?.status
    
    if (myStatus === 'SUBMITTED' || myStatus === 'GRADED') {
      return false
    }
    
    if (status === 'PUBLISHED' || status === 'IN_PROGRESS') {
      if (e.endTime && new Date() > new Date(e.endTime)) {
        return false
      }
      if (e.startTime && new Date() < new Date(e.startTime)) {
        return false
      }
      return true
    }
    return false
  })
})

const completedExams = computed(() => {
  return filteredExams.value.filter(e => {
    const myStatus = e.mySubmission?.status
    if (myStatus === 'SUBMITTED' || myStatus === 'GRADED') {
      return true
    }
    if (e.status === 'ENDED' || e.status === 'ARCHIVED') {
      return true
    }
    if (e.endTime && new Date() > new Date(e.endTime)) {
      return true
    }
    return false
  })
})

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    IN_PROGRESS: 'primary',
    ENDED: 'warning',
    ARCHIVED: 'info'
  }
  return map[status] || 'info'
}

const getSubmissionStatusType = (status) => {
  const map = {
    NOT_STARTED: 'info',
    IN_PROGRESS: 'warning',
    SUBMITTED: 'warning',
    GRADED: 'success'
  }
  return map[status] || 'info'
}

const getSubmissionStatusText = (status) => {
  const map = {
    NOT_STARTED: '未开始',
    IN_PROGRESS: '进行中',
    SUBMITTED: '已提交',
    GRADED: '已评分'
  }
  return map[status] || '未开始'
}

const formatDateTime = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const canTakeExam = (row) => {
  const status = row.status
  const myStatus = row.mySubmission?.status
  
  if (myStatus === 'SUBMITTED' || myStatus === 'GRADED') {
    return false
  }
  
  if (row.endTime && new Date() > new Date(row.endTime)) {
    return false
  }
  if (row.startTime && new Date() < new Date(row.startTime)) {
    return false
  }
  
  return status === 'PUBLISHED' || status === 'IN_PROGRESS'
}

const isTimeExpired = (row) => {
  return row.endTime && new Date() > new Date(row.endTime)
}

const fetchCourses = async () => {
  try {
    const res = await getMyCourses()
    courses.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchExams = async () => {
  loading.value = true
  try {
    const res = await getStudentExams()
    allExams.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取考试列表失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tab) => {
  if (tab === 'completed') {
    completedLoading.value = true
    setTimeout(() => {
      completedLoading.value = false
    }, 300)
  }
}

const startExam = async (row) => {
  router.push(`/student-exam/take/${row.id}`)
}

const viewResult = (row) => {
  router.push(`/student-exam/result/${row.id}`)
}

onMounted(() => {
  fetchCourses()
  fetchExams()
})
</script>

<style scoped>
.student-exam {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.el-tabs__content) {
  padding-top: 16px;
}
</style>
