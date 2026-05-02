<template>
  <div class="homework-management">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="作业列表" name="list">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>作业列表</span>
              <div class="header-actions">
                <el-select v-model="filterCourseId" placeholder="选择课程" clearable style="width: 200px; margin-right: 10px">
                  <el-option
                    v-for="course in courses"
                    :key="course.id"
                    :label="course.courseName"
                    :value="course.id"
                  />
                </el-select>
                <el-button type="primary" @click="openCreateDialog">
                  <el-icon><Plus /></el-icon>
                  发布新作业
                </el-button>
              </div>
            </div>
          </template>

          <el-table :data="homeworks" v-loading="loading" stripe>
            <el-table-column prop="title" label="作业标题" min-width="200" />
            <el-table-column prop="courseName" label="所属课程" width="180" />
            <el-table-column label="截止时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.deadline) }}
              </template>
            </el-table-column>
            <el-table-column label="提交情况" width="120">
              <template #default="{ row }">
                <span>{{ row.submittedCount }}/{{ row.totalStudents }}</span>
              </template>
            </el-table-column>
            <el-table-column label="已评分" width="100">
              <template #default="{ row }">
                <span>{{ row.gradedCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="提醒设置" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.enableReminder" type="success">
                  开启 ({{ row.reminderMinutes }}分钟前)
                </el-tag>
                <el-tag v-else type="info">
                  关闭
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="viewSubmissions(row)">
                  查看提交
                </el-button>
                <el-button type="warning" link @click="editHomework(row)">
                  编辑
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  type="success"
                  link
                  @click="publishHomework(row)"
                >
                  发布
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && homeworks.length === 0" description="暂无作业" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="homeworkDialogVisible"
      :title="isEditing ? '编辑作业' : '发布新作业'"
      width="600px"
    >
      <el-form :model="homeworkForm" label-width="100px">
        <el-form-item label="课程" required>
          <el-select v-model="homeworkForm.courseId" placeholder="选择课程" style="width: 100%">
            <el-option
              v-for="course in courses"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="作业标题" required>
          <el-input v-model="homeworkForm.title" placeholder="请输入作业标题" />
        </el-form-item>
        <el-form-item label="作业描述">
          <el-input
            v-model="homeworkForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入作业描述（可选）"
          />
        </el-form-item>
        <el-form-item label="截止时间" required>
          <el-date-picker
            v-model="homeworkForm.deadline"
            type="datetime"
            placeholder="选择截止时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="开启提醒">
          <el-switch v-model="homeworkForm.enableReminder" />
        </el-form-item>
        <el-form-item v-if="homeworkForm.enableReminder" label="提前提醒">
          <el-select v-model="homeworkForm.reminderMinutes" style="width: 200px">
            <el-option :value="1" label="1分钟前" />
            <el-option :value="3" label="3分钟前" />
            <el-option :value="5" label="5分钟前" />
            <el-option :value="10" label="10分钟前" />
            <el-option :value="30" label="30分钟前" />
            <el-option :value="60" label="1小时前" />
            <el-option :value="1440" label="1天前" />
          </el-select>
          <span style="margin-left: 10px; color: #909399">提醒学生提交作业</span>
        </el-form-item>
        <el-form-item v-if="!isEditing" label="立即发布">
          <el-switch v-model="homeworkForm.publishNow" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="homeworkDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHomework" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="submissionsDialogVisible"
      title="查看作业提交"
      width="800px"
    >
      <template #header>
        <div class="dialog-header">
          <span>{{ currentHomework?.title }}</span>
          <span style="font-size: 14px; color: #909399; margin-left: 10px">
            截止时间: {{ formatDateTime(currentHomework?.deadline) }}
          </span>
        </div>
      </template>

      <el-table :data="submissions" v-loading="submissionsLoading" stripe>
        <el-table-column prop="studentName" label="学生姓名" width="100" />
        <el-table-column prop="studentUsername" label="学号" width="120" />
        <el-table-column label="提交状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getSubmissionStatusType(row.status)">
              {{ getSubmissionStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否迟交" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isLate" type="danger">是</el-tag>
            <el-tag v-else type="info">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">
            {{ row.submittedAt ? formatDateTime(row.submittedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="分数" width="80">
          <template #default="{ row }">
            <span v-if="row.score !== null">{{ row.score }}</span>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column label="教师评语" show-overflow-tooltip min-width="150">
          <template #default="{ row }">
            {{ row.teacherComment || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'SUBMITTED'"
              type="primary"
              link
              @click="openGradeDialog(row)"
            >
              评分
            </el-button>
            <el-button
              v-if="row.status === 'SUBMITTED' || row.status === 'GRADED'"
              type="warning"
              link
              @click="viewSubmissionDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      v-model="gradeDialogVisible"
      title="作业评分"
      width="500px"
    >
      <el-descriptions :column="1" border size="small" v-if="currentSubmission">
        <el-descriptions-item label="学生">
          {{ currentSubmission.studentName }} ({{ currentSubmission.studentUsername }})
        </el-descriptions-item>
        <el-descriptions-item label="提交内容" v-if="currentSubmission.content">
          {{ currentSubmission.content }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ formatDateTime(currentSubmission.submittedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="是否迟交">
          <el-tag :type="currentSubmission.isLate ? 'danger' : 'info'">
            {{ currentSubmission.isLate ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-form :model="gradeForm" label-width="80px" style="margin-top: 20px">
        <el-form-item label="分数">
          <el-input-number
            v-model="gradeForm.score"
            :min="0"
            :max="100"
            :precision="1"
            placeholder="请输入分数"
          />
          <span style="margin-left: 10px; color: #909399">分</span>
        </el-form-item>
        <el-form-item label="评语">
          <el-input
            v-model="gradeForm.teacherComment"
            type="textarea"
            :rows="3"
            placeholder="请输入评语（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="gradeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGrade" :loading="grading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="作业提交详情"
      width="600px"
    >
      <el-descriptions :column="1" border v-if="currentSubmission">
        <el-descriptions-item label="学生">
          {{ currentSubmission.studentName }} ({{ currentSubmission.studentUsername }})
        </el-descriptions-item>
        <el-descriptions-item label="提交状态">
          <el-tag :type="getSubmissionStatusType(currentSubmission.status)">
            {{ getSubmissionStatusText(currentSubmission.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交内容" v-if="currentSubmission.content">
          {{ currentSubmission.content }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ formatDateTime(currentSubmission.submittedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="是否迟交">
          <el-tag :type="currentSubmission.isLate ? 'danger' : 'info'">
            {{ currentSubmission.isLate ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="分数" v-if="currentSubmission.score !== null">
          {{ currentSubmission.score }} 分
        </el-descriptions-item>
        <el-descriptions-item label="教师评语" v-if="currentSubmission.teacherComment">
          {{ currentSubmission.teacherComment }}
        </el-descriptions-item>
        <el-descriptions-item label="评分时间" v-if="currentSubmission.gradedAt">
          {{ formatDateTime(currentSubmission.gradedAt) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getTeacherCourses } from '@/api/courses'
import {
  getTeacherHomeworks,
  createHomework,
  updateHomework,
  publishHomework as publishHomeworkApi,
  getHomeworkSubmissions,
  gradeHomework
} from '@/api/homework'

const activeTab = ref('list')
const courses = ref([])
const homeworks = ref([])
const loading = ref(false)
const submissionsLoading = ref(false)

const filterCourseId = ref(null)

const homeworkDialogVisible = ref(false)
const isEditing = ref(false)
const submitting = ref(false)
const homeworkForm = ref({
  courseId: null,
  title: '',
  description: '',
  deadline: null,
  enableReminder: true,
  reminderMinutes: 3,
  publishNow: false
})

const submissionsDialogVisible = ref(false)
const currentHomework = ref(null)
const submissions = ref([])

const gradeDialogVisible = ref(false)
const currentSubmission = ref(null)
const grading = ref(false)
const gradeForm = ref({
  score: null,
  teacherComment: ''
})

const detailDialogVisible = ref(false)

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    ARCHIVED: 'warning'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档'
  }
  return map[status] || status
}

const getSubmissionStatusType = (status) => {
  const map = {
    NOT_SUBMITTED: 'info',
    SUBMITTED: 'warning',
    GRADED: 'success'
  }
  return map[status] || ''
}

const getSubmissionStatusText = (status) => {
  const map = {
    NOT_SUBMITTED: '未提交',
    SUBMITTED: '已提交',
    GRADED: '已评分'
  }
  return map[status] || status
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

const fetchHomeworks = async () => {
  loading.value = true
  try {
    const res = await getTeacherHomeworks()
    let data = res.data
    
    if (filterCourseId.value) {
      data = data.filter(h => h.courseId === filterCourseId.value)
    }
    
    homeworks.value = data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取作业列表失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEditing.value = false
  homeworkForm.value = {
    courseId: courses.value.length > 0 ? courses.value[0].id : null,
    title: '',
    description: '',
    deadline: null,
    enableReminder: true,
    reminderMinutes: 3,
    publishNow: false
  }
  homeworkDialogVisible.value = true
}

const editHomework = (row) => {
  isEditing.value = true
  homeworkForm.value = {
    courseId: row.courseId,
    title: row.title,
    description: row.description || '',
    deadline: row.deadline,
    enableReminder: row.enableReminder,
    reminderMinutes: row.reminderMinutes,
    publishNow: false
  }
  homeworkDialogVisible.value = true
}

const submitHomework = async () => {
  if (!homeworkForm.value.courseId) {
    ElMessage.warning('请选择课程')
    return
  }
  if (!homeworkForm.value.title.trim()) {
    ElMessage.warning('请输入作业标题')
    return
  }
  if (!homeworkForm.value.deadline) {
    ElMessage.warning('请选择截止时间')
    return
  }

  submitting.value = true
  try {
    const data = {
      courseId: homeworkForm.value.courseId,
      title: homeworkForm.value.title,
      description: homeworkForm.value.description,
      deadline: homeworkForm.value.deadline,
      enableReminder: homeworkForm.value.enableReminder,
      reminderMinutes: homeworkForm.value.reminderMinutes,
      status: isEditing.value ? 'PUBLISHED' : (homeworkForm.value.publishNow ? 'PUBLISHED' : 'DRAFT')
    }

    if (isEditing.value) {
      await updateHomework(currentHomework.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createHomework(data)
      ElMessage.success(homeworkForm.value.publishNow ? '发布成功' : '已保存为草稿')
    }

    homeworkDialogVisible.value = false
    fetchHomeworks()
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const publishHomework = async (row) => {
  try {
    await ElMessageBox.confirm('确定要发布该作业吗？发布后学生可以看到并提交作业。', '确认发布', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await publishHomeworkApi(row.id)
    ElMessage.success('发布成功')
    fetchHomeworks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error.response?.data?.message || '发布失败')
    }
  }
}

const viewSubmissions = async (row) => {
  currentHomework.value = row
  submissionsDialogVisible.value = true
  await fetchSubmissions()
}

const fetchSubmissions = async () => {
  if (!currentHomework.value) return
  
  submissionsLoading.value = true
  try {
    const res = await getHomeworkSubmissions(currentHomework.value.id)
    submissions.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取提交列表失败')
  } finally {
    submissionsLoading.value = false
  }
}

const openGradeDialog = (row) => {
  currentSubmission.value = row
  gradeForm.value = {
    score: row.score,
    teacherComment: row.teacherComment || ''
  }
  gradeDialogVisible.value = true
}

const viewSubmissionDetail = (row) => {
  currentSubmission.value = row
  detailDialogVisible.value = true
}

const submitGrade = async () => {
  if (gradeForm.value.score === null || gradeForm.value.score === '') {
    ElMessage.warning('请输入分数')
    return
  }

  grading.value = true
  try {
    await gradeHomework(currentSubmission.value.id, {
      score: gradeForm.value.score,
      teacherComment: gradeForm.value.teacherComment
    })
    ElMessage.success('评分成功')
    gradeDialogVisible.value = false
    fetchSubmissions()
    fetchHomeworks()
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '评分失败')
  } finally {
    grading.value = false
  }
}

onMounted(() => {
  fetchCourses()
  fetchHomeworks()
})
</script>

<style scoped>
.homework-management {
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
