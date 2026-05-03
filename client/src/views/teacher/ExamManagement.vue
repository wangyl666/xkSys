<template>
  <div class="exam-management">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="考试列表" name="list">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>考试管理</span>
              <div class="header-actions">
                <el-select v-model="filterCourseId" placeholder="选择课程" clearable style="width: 180px; margin-right: 10px">
                  <el-option
                    v-for="course in courses"
                    :key="course.id"
                    :label="course.courseName"
                    :value="course.id"
                  />
                </el-select>
                <el-select v-model="filterStatus" placeholder="考试状态" clearable style="width: 140px; margin-right: 10px">
                  <el-option
                    v-for="status in examStatuses"
                    :key="status.value"
                    :label="status.label"
                    :value="status.value"
                  />
                </el-select>
                <el-button type="primary" @click="openCreateDialog">
                  <el-icon><Plus /></el-icon>
                  发布考试
                </el-button>
              </div>
            </div>
          </template>

          <el-table :data="filteredExams" v-loading="loading" stripe>
            <el-table-column prop="title" label="考试标题" min-width="200" />
            <el-table-column prop="courseName" label="所属课程" width="120" />
            <el-table-column prop="examPaperTitle" label="使用试卷" width="150" show-overflow-tooltip />
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
            <el-table-column label="提交情况" width="100">
              <template #default="{ row }">
                <span>{{ row.submittedCount || 0 }}/{{ row.totalStudents || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="已评分" width="80">
              <template #default="{ row }">
                <span>{{ row.gradedCount || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="statusName" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ row.statusName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成绩公布" width="100">
              <template #default="{ row }">
                <el-tag :type="row.scoresPublished ? 'success' : 'info'" size="small">
                  {{ row.scoresPublished ? '已公布' : '未公布' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="viewExam(row)">
                  查看
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  type="warning"
                  link
                  @click="editExam(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  type="success"
                  link
                  @click="handlePublishExam(row)"
                >
                  发布
                </el-button>
                <el-button
                  v-if="row.status === 'PUBLISHED' || row.status === 'IN_PROGRESS' || row.status === 'ENDED'"
                  type="info"
                  link
                  @click="viewSubmissions(row)"
                >
                  查看提交
                </el-button>
                <el-button
                  v-if="(row.status === 'ENDED' || row.status === 'PUBLISHED') && row.submittedCount > 0 && !row.scoresPublished"
                  type="warning"
                  link
                  @click="goToGrading(row)"
                >
                  阅卷
                </el-button>
                <el-button
                  v-if="row.gradedCount === row.submittedCount && row.submittedCount > 0 && !row.scoresPublished"
                  type="success"
                  link
                  @click="handlePublishScores(row)"
                >
                  公布成绩
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && exams.length === 0" description="暂无考试" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="createDialogVisible"
      title="发布考试"
      width="600px"
    >
      <el-form :model="examForm" label-width="100px">
        <el-form-item label="选择试卷" required>
          <el-select v-model="examForm.examPaperId" placeholder="请选择试卷" style="width: 100%">
            <el-option
              v-for="paper in examPapers"
              :key="paper.id"
              :label="paper.title"
              :value="paper.id"
            >
              <span>{{ paper.title }}</span>
              <span style="color: #909399; margin-left: 8px; font-size: 12px">
                ({{ paper.questionCount }}题, {{ paper.totalScore }}分)
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="考试标题" required>
          <el-input v-model="examForm.title" placeholder="请输入考试标题（留空则使用试卷标题）" />
        </el-form-item>
        <el-form-item label="所属课程">
          <el-select v-model="examForm.courseId" placeholder="选择课程（可选）" style="width: 100%">
            <el-option
              v-for="course in courses"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="考试时长">
          <el-input-number v-model="examForm.duration" :min="10" :max="300" />
          <span style="margin-left: 10px; color: #909399">分钟（留空则使用试卷设置）</span>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-date-picker
                v-model="examForm.startTime"
                type="datetime"
                placeholder="选择开始时间（可选）"
                style="width: 100%"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-date-picker
                v-model="examForm.endTime"
                type="datetime"
                placeholder="选择结束时间（可选）"
                style="width: 100%"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="立即发布">
          <el-switch v-model="examForm.publishNow" />
          <span style="margin-left: 10px; color: #909399">发布后学生即可参加考试</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExam" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="考试详情"
      width="800px"
    >
      <el-descriptions :column="2" border v-if="currentExam">
        <el-descriptions-item label="考试标题">{{ currentExam.title }}</el-descriptions-item>
        <el-descriptions-item label="所属课程">{{ currentExam.courseName || '未关联课程' }}</el-descriptions-item>
        <el-descriptions-item label="使用试卷">{{ currentExam.examPaperTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentExam.totalScore }} 分</el-descriptions-item>
        <el-descriptions-item label="考试时长">{{ currentExam.duration }} 分钟</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentExam.status)">
            {{ currentExam.statusName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间" v-if="currentExam.startTime">
          {{ formatDateTime(currentExam.startTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间" v-if="currentExam.endTime">
          {{ formatDateTime(currentExam.endTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="提交情况">
          已提交 {{ currentExam.submittedCount || 0 }} 人 / 共 {{ currentExam.totalStudents || 0 }} 人
        </el-descriptions-item>
        <el-descriptions-item label="成绩公布">
          <el-tag :type="currentExam.scoresPublished ? 'success' : 'info'">
            {{ currentExam.scoresPublished ? '已公布' : '未公布' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left" v-if="currentExam?.questions">题目列表</el-divider>

      <el-table :data="currentExam?.questions || []" stripe size="small" v-if="currentExam?.questions">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="typeName" label="类型" width="80" />
        <el-table-column prop="content" label="题目内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="80" />
        <el-table-column prop="difficulty" label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)" size="small">
              {{ row.difficulty || '-' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      v-model="submissionsDialogVisible"
      title="考试提交列表"
      width="1000px"
    >
      <template #header>
        <div class="dialog-header">
          <span>{{ currentExam?.title }}</span>
          <span style="font-size: 14px; color: #909399; margin-left: 10px">
            提交情况: {{ filteredSubmissions.length }} 人
          </span>
        </div>
      </template>

      <el-table :data="filteredSubmissions" v-loading="submissionsLoading" stripe>
        <el-table-column prop="studentName" label="学生姓名" width="100" />
        <el-table-column prop="studentUsername" label="学号" width="120" />
        <el-table-column label="提交状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getSubmissionStatusType(row.status)">
              {{ getSubmissionStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">
            {{ row.startedAt ? formatDateTime(row.startedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">
            {{ row.submittedAt ? formatDateTime(row.submittedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="客观题得分" width="100">
          <template #default="{ row }">
            <span v-if="row.objectiveScore !== null">{{ row.objectiveScore }}</span>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column label="主观题得分" width="100">
          <template #default="{ row }">
            <span v-if="row.subjectiveScore !== null">{{ row.subjectiveScore }}</span>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="100">
          <template #default="{ row }">
            <span v-if="row.totalScore !== null" style="font-weight: bold; color: #409eff">
              {{ row.totalScore }}
            </span>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'SUBMITTED'"
              type="primary"
              link
              @click="goToStudentGrading(row)"
            >
              阅卷
            </el-button>
            <el-button
              v-if="row.status === 'GRADED'"
              type="warning"
              link
              @click="viewSubmissionDetail(row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getTeacherCourses } from '@/api/courses'
import { getTeacherExamPapers } from '@/api/examPapers'
import {
  getTeacherExams,
  getExamById,
  getExamSubmissions,
  createExam,
  publishExam as apiPublishExam,
  publishScores as apiPublishScores,
  getExamStatuses
} from '@/api/exams'

const router = useRouter()

const activeTab = ref('list')
const courses = ref([])
const exams = ref([])
const examPapers = ref([])
const examStatuses = ref([])
const loading = ref(false)
const submissionsLoading = ref(false)

const filterCourseId = ref(null)
const filterStatus = ref(null)

const filteredExams = computed(() => {
  let result = exams.value
  if (filterCourseId.value) {
    result = result.filter(e => e.courseId === filterCourseId.value)
  }
  if (filterStatus.value) {
    result = result.filter(e => e.status === filterStatus.value)
  }
  return result
})

const filteredSubmissions = computed(() => {
  return submissions.value
})

const createDialogVisible = ref(false)
const submitting = ref(false)
const examForm = ref({
  examPaperId: null,
  title: '',
  courseId: null,
  duration: null,
  startTime: null,
  endTime: null,
  publishNow: false
})

const detailDialogVisible = ref(false)
const currentExam = ref(null)

const submissionsDialogVisible = ref(false)
const submissions = ref([])

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

const getDifficultyType = (difficulty) => {
  const map = {
    '简单': 'success',
    '中等': 'warning',
    '困难': 'danger'
  }
  return map[difficulty] || 'info'
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
  return map[status] || '未知'
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

const fetchCourses = async () => {
  try {
    const res = await getTeacherCourses()
    courses.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchExamPapers = async () => {
  try {
    const res = await getTeacherExamPapers()
    examPapers.value = res.data.filter(p => p.status === 'PUBLISHED' || p.status === 'DRAFT')
  } catch (error) {
    console.error(error)
  }
}

const fetchExamStatuses = async () => {
  try {
    const res = await getExamStatuses()
    examStatuses.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchExams = async () => {
  loading.value = true
  try {
    const res = await getTeacherExams(filterCourseId.value)
    exams.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取考试列表失败')
  } finally {
    loading.value = false
  }
}

watch(filterCourseId, () => {
  fetchExams()
})

const openCreateDialog = () => {
  examForm.value = {
    examPaperId: examPapers.value.length > 0 ? examPapers.value[0].id : null,
    title: '',
    courseId: courses.value.length > 0 ? courses.value[0].id : null,
    duration: null,
    startTime: null,
    endTime: null,
    publishNow: false
  }
  createDialogVisible.value = true
}

const submitExam = async () => {
  if (!examForm.value.examPaperId) {
    ElMessage.warning('请选择试卷')
    return
  }

  const selectedPaper = examPapers.value.find(p => p.id === examForm.value.examPaperId)
  if (!selectedPaper) {
    ElMessage.warning('请选择有效的试卷')
    return
  }

  submitting.value = true
  try {
    const data = {
      examPaperId: examForm.value.examPaperId,
      title: examForm.value.title || selectedPaper.title,
      courseId: examForm.value.courseId,
      totalScore: selectedPaper.totalScore,
      duration: examForm.value.duration || selectedPaper.duration,
      startTime: examForm.value.startTime,
      endTime: examForm.value.endTime,
      status: examForm.value.publishNow ? 'PUBLISHED' : 'DRAFT'
    }

    await createExam(data)
    ElMessage.success(examForm.value.publishNow ? '发布成功' : '已保存为草稿')
    createDialogVisible.value = false
    fetchExams()
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const viewExam = async (row) => {
  try {
    const res = await getExamById(row.id)
    currentExam.value = res.data
    detailDialogVisible.value = true
  } catch (error) {
    console.error(error)
    ElMessage.error('获取考试详情失败')
  }
}

const editExam = async (row) => {
  try {
    const res = await getExamById(row.id)
    examForm.value = {
      examPaperId: res.data.examPaperId,
      title: res.data.title,
      courseId: res.data.courseId,
      duration: res.data.duration,
      startTime: res.data.startTime,
      endTime: res.data.endTime,
      publishNow: false
    }
    createDialogVisible.value = true
  } catch (error) {
    console.error(error)
    ElMessage.error('获取考试详情失败')
  }
}

const handlePublishExam = async (row) => {
  try {
    await ElMessageBox.confirm('确定要发布该考试吗？发布后学生可以参加考试。', '确认发布', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await apiPublishExam(row.id)
    ElMessage.success('发布成功')
    fetchExams()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error.response?.data?.message || '发布失败')
    }
  }
}

const viewSubmissions = async (row) => {
  currentExam.value = row
  submissionsDialogVisible.value = true
  await fetchSubmissions(row.id)
}

const fetchSubmissions = async (examId) => {
  submissionsLoading.value = true
  try {
    const res = await getExamSubmissions(examId)
    submissions.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取提交列表失败')
  } finally {
    submissionsLoading.value = false
  }
}

const goToGrading = (row) => {
  router.push(`/exam-grading/${row.id}`)
}

const goToStudentGrading = (row) => {
  currentExam.value = row.exam
  submissionsDialogVisible.value = false
  router.push(`/exam-grading/${row.examId}?submission=${row.id}`)
}

const viewSubmissionDetail = (row) => {
  ElMessage.info('请前往阅卷页面查看详情')
}

const handlePublishScores = async (row) => {
  try {
    await ElMessageBox.confirm('确定要公布成绩吗？公布后所有学生都可以查看自己的成绩。', '确认公布', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await apiPublishScores(row.id)
    ElMessage.success('成绩已公布')
    fetchExams()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error.response?.data?.message || '公布失败')
    }
  }
}

onMounted(() => {
  fetchCourses()
  fetchExamPapers()
  fetchExamStatuses()
  fetchExams()
})
</script>

<style scoped>
.exam-management {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
}

.dialog-header {
  display: flex;
  align-items: center;
}

:deep(.el-tabs__content) {
  padding-top: 16px;
}
</style>
