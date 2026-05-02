<template>
  <div class="student-homework">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="待完成作业" name="pending">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>待完成作业</span>
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

          <el-table :data="pendingHomeworks" v-loading="loading" stripe>
            <el-table-column prop="title" label="作业标题" min-width="200" />
            <el-table-column prop="courseName" label="所属课程" width="180" />
            <el-table-column prop="teacherName" label="任课老师" width="120" />
            <el-table-column label="截止时间" width="180">
              <template #default="{ row }">
                <span :class="{ 'expired-text': isExpired(row.deadline) }">
                  {{ formatDateTime(row.deadline) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="剩余时间" width="150">
              <template #default="{ row }">
                <el-tag v-if="isExpired(row.deadline)" type="danger">已过期</el-tag>
                <el-tag v-else :type="getTimeType(row.deadline)">
                  {{ getRemainingTime(row.deadline) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getSubmissionStatusType(row.mySubmission?.status)">
                  {{ getSubmissionStatusText(row.mySubmission?.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  @click="openSubmitDialog(row)"
                >
                  {{ row.mySubmission?.status === 'GRADED' ? '查看详情' : 
                     row.mySubmission?.status === 'SUBMITTED' ? '查看/修改' : '提交作业' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && pendingHomeworks.length === 0" description="暂无待完成作业" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="已完成作业" name="completed">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>已完成作业</span>
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

          <el-table :data="completedHomeworks" v-loading="completedLoading" stripe>
            <el-table-column prop="title" label="作业标题" min-width="200" />
            <el-table-column prop="courseName" label="所属课程" width="180" />
            <el-table-column prop="teacherName" label="任课老师" width="120" />
            <el-table-column label="截止时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.deadline) }}
              </template>
            </el-table-column>
            <el-table-column label="提交状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getSubmissionStatusType(row.mySubmission?.status)">
                  {{ getSubmissionStatusText(row.mySubmission?.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="分数" width="80">
              <template #default="{ row }">
                <span v-if="row.mySubmission?.score !== null">{{ row.mySubmission.score }}</span>
                <span v-else style="color: #909399">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  @click="openSubmitDialog(row)"
                >
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!completedLoading && completedHomeworks.length === 0" description="暂无已完成作业" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="submitDialogVisible"
      :title="isViewOnly ? '作业详情' : '提交作业'"
      width="600px"
    >
      <el-descriptions :column="1" border v-if="currentHomework">
        <el-descriptions-item label="作业标题">
          {{ currentHomework.title }}
        </el-descriptions-item>
        <el-descriptions-item label="所属课程">
          {{ currentHomework.courseName }}
        </el-descriptions-item>
        <el-descriptions-item label="任课老师">
          {{ currentHomework.teacherName }}
        </el-descriptions-item>
        <el-descriptions-item label="截止时间">
          <span :class="{ 'expired-text': isExpired(currentHomework.deadline) }">
            {{ formatDateTime(currentHomework.deadline) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="作业描述" v-if="currentHomework.description">
          {{ currentHomework.description }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">提交内容</el-divider>

      <el-form :model="submitForm" label-width="80px">
        <el-form-item label="当前状态" v-if="currentSubmission">
          <el-tag :type="getSubmissionStatusType(currentSubmission.status)">
            {{ getSubmissionStatusText(currentSubmission.status) }}
            <span v-if="currentSubmission.isLate" style="margin-left: 8px; color: #f56c6c">(迟交)</span>
          </el-tag>
        </el-form-item>
        <el-form-item label="提交时间" v-if="currentSubmission?.submittedAt">
          {{ formatDateTime(currentSubmission.submittedAt) }}
        </el-form-item>
        <el-form-item label="作业内容" v-if="!isViewOnly">
          <el-input
            v-model="submitForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入作业内容（可选）"
          />
        </el-form-item>
        <el-form-item label="作业内容" v-else-if="currentSubmission?.content">
          <div style="white-space: pre-wrap; padding: 10px; background-color: #f5f7fa; border-radius: 4px;">
            {{ currentSubmission.content }}
          </div>
        </el-form-item>
        <el-form-item label="分数" v-if="currentSubmission?.score !== null">
          <span style="font-size: 20px; font-weight: bold; color: #409eff">
            {{ currentSubmission.score }} 分
          </span>
        </el-form-item>
        <el-form-item label="教师评语" v-if="currentSubmission?.teacherComment">
          <div style="padding: 10px; background-color: #fdf6ec; border-radius: 4px; color: #e6a23c;">
            {{ currentSubmission.teacherComment }}
          </div>
        </el-form-item>
        <el-form-item label="评分时间" v-if="currentSubmission?.gradedAt">
          {{ formatDateTime(currentSubmission.gradedAt) }}
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="submitDialogVisible = false">
          {{ isViewOnly ? '关闭' : '取消' }}
        </el-button>
        <el-button
          v-if="!isViewOnly && !isExpired(currentHomework?.deadline)"
          type="primary"
          @click="submitHomework"
          :loading="submitting"
        >
          {{ currentSubmission?.status === 'SUBMITTED' ? '修改提交' : '提交作业' }}
        </el-button>
        <el-button
          v-if="!isViewOnly && isExpired(currentHomework?.deadline)"
          type="danger"
          disabled
        >
          已截止
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getStudentHomeworks, submitHomework, getMySubmission } from '@/api/homework'
import { getMyCourses } from '@/api/enrollments'

const activeTab = ref('pending')
const courses = ref([])
const allHomeworks = ref([])
const loading = ref(false)
const completedLoading = ref(false)

const filterCourseId = ref(null)

const submitDialogVisible = ref(false)
const currentHomework = ref(null)
const currentSubmission = ref(null)
const submitForm = ref({
  content: ''
})
const submitting = ref(false)

const isViewOnly = computed(() => {
  if (!currentSubmission.value) return false
  return currentSubmission.value.status === 'GRADED' || 
         (currentHomework.value && isExpired(currentHomework.value.deadline) && 
          currentSubmission.value.status === 'NOT_SUBMITTED')
})

const pendingHomeworks = computed(() => {
  let data = allHomeworks.value.filter(h => 
    (h.mySubmission?.status !== 'GRADED') && !isExpired(h.deadline)
  )
  
  if (filterCourseId.value) {
    data = data.filter(h => h.courseId === filterCourseId.value)
  }
  
  return data.sort((a, b) => new Date(a.deadline) - new Date(b.deadline))
})

const completedHomeworks = computed(() => {
  let data = allHomeworks.value.filter(h => 
    h.mySubmission?.status === 'GRADED' || isExpired(h.deadline)
  )
  
  if (filterCourseId.value) {
    data = data.filter(h => h.courseId === filterCourseId.value)
  }
  
  return data.sort((a, b) => new Date(b.deadline) - new Date(a.deadline))
})

const getSubmissionStatusType = (status) => {
  const map = {
    NOT_SUBMITTED: 'info',
    SUBMITTED: 'warning',
    GRADED: 'success'
  }
  return map[status] || 'info'
}

const getSubmissionStatusText = (status) => {
  const map = {
    NOT_SUBMITTED: '未提交',
    SUBMITTED: '已提交',
    GRADED: '已评分'
  }
  return map[status] || '未提交'
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

const isExpired = (deadline) => {
  if (!deadline) return false
  return new Date() > new Date(deadline)
}

const getTimeType = (deadline) => {
  if (!deadline) return 'info'
  const now = new Date()
  const deadlineTime = new Date(deadline)
  const diff = deadlineTime - now
  
  if (diff < 0) return 'danger'
  if (diff < 5 * 60 * 1000) return 'danger'
  if (diff < 30 * 60 * 1000) return 'warning'
  return 'info'
}

const getRemainingTime = (deadline) => {
  if (!deadline) return '-'
  const now = new Date()
  const deadlineTime = new Date(deadline)
  const diff = deadlineTime - now
  
  if (diff <= 0) return '已过期'
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  
  if (days > 0) return `${days}天${hours % 24}小时`
  if (hours > 0) return `${hours}小时${minutes % 60}分钟`
  return `${minutes}分钟`
}

const fetchCourses = async () => {
  try {
    const res = await getMyCourses()
    courses.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchHomeworks = async () => {
  loading.value = true
  try {
    const res = await getStudentHomeworks()
    allHomeworks.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取作业列表失败')
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

const openSubmitDialog = async (row) => {
  currentHomework.value = row
  submitForm.value = {
    content: ''
  }
  
  try {
    const res = await getMySubmission(row.id)
    if (res.data) {
      currentSubmission.value = res.data
      submitForm.value.content = res.data.content || ''
    } else {
      currentSubmission.value = {
        status: 'NOT_SUBMITTED'
      }
    }
  } catch (error) {
    console.error(error)
    currentSubmission.value = {
      status: 'NOT_SUBMITTED'
    }
  }
  
  submitDialogVisible.value = true
}

const submitHomework = async () => {
  if (!currentHomework.value) return
  
  submitting.value = true
  try {
    await submitHomework(currentHomework.value.id, {
      content: submitForm.value.content,
      fileUrl: null
    })
    ElMessage.success('提交成功')
    submitDialogVisible.value = false
    fetchHomeworks()
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchCourses()
  fetchHomeworks()
})
</script>

<style scoped>
.student-homework {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.expired-text {
  color: #f56c6c;
  text-decoration: line-through;
}

:deep(.el-tabs__content) {
  padding-top: 16px;
}
</style>
