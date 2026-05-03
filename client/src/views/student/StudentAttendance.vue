<template>
  <div class="student-attendance">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="考勤记录" name="records">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的考勤记录</span>
              <el-select v-model="filterStatus" placeholder="筛选状态" clearable style="width: 150px" @change="fetchAttendances">
                <el-option label="全部" value="" />
                <el-option label="出勤" value="PRESENT" />
                <el-option label="缺勤" value="ABSENT" />
                <el-option label="迟到" value="LATE" />
                <el-option label="事假" value="EXCUSED" />
              </el-select>
            </div>
          </template>

          <el-table :data="attendances" v-loading="loading" stripe>
            <el-table-column prop="courseName" label="课程名称" width="180" />
            <el-table-column label="考勤日期" width="120">
              <template #default="{ row }">
                {{ row.attendanceDate }}
              </template>
            </el-table-column>
            <el-table-column label="周几" width="80">
              <template #default="{ row }">
                {{ row.dayOfWeek }}
              </template>
            </el-table-column>
            <el-table-column label="节次" width="100">
              <template #default="{ row }">
                第{{ row.startSection }}-{{ row.endSection }}节
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="是否有申诉" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.hasAppeal" type="warning">待审核</el-tag>
                <el-tag v-else-if="row.appealStatus" :type="getAppealTagType(row.appealStatus)">
                  {{ getAppealStatusText(row.appealStatus) }}
                </el-tag>
                <el-tag v-else type="info">无</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  @click="createAppeal(row)"
                  :disabled="row.status !== 'ABSENT' || row.hasAppeal || row.appealStatus"
                >
                  申诉
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && attendances.length === 0" description="暂无考勤记录" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="缺勤通知" name="notifications">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>缺勤通知</span>
              <el-button type="primary" link @click="markAllRead" v-if="unreadCount > 0">
                全部已读
              </el-button>
              <el-badge :value="unreadCount" class="item" v-if="unreadCount > 0">
                <span style="visibility: hidden">未读</span>
              </el-badge>
            </div>
          </template>

          <el-table :data="notifications" v-loading="notificationsLoading" stripe>
            <el-table-column label="课程名称" width="180">
              <template #default="{ row }">
                <div :class="{ 'unread-item': row.status === 'UNREAD' }">
                  <el-badge is-dot v-if="row.status === 'UNREAD'" />
                  {{ row.courseName }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="通知内容" show-overflow-tooltip>
              <template #default="{ row }">
                <div :class="{ 'unread-item': row.status === 'UNREAD' }">
                  {{ row.message }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="发送时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.sentAt) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'UNREAD' ? 'danger' : 'info'">
                  {{ row.status === 'UNREAD' ? '未读' : '已读' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  @click="markAsRead(row)"
                  :disabled="row.status === 'READ'"
                >
                  标为已读
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!notificationsLoading && notifications.length === 0" description="暂无通知" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="我的申诉" name="appeals">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的申诉记录</span>
              <el-radio-group v-model="appealStatusFilter" size="small" @change="fetchAppeals">
                <el-radio-button value="">全部</el-radio-button>
                <el-radio-button value="PENDING">待审核</el-radio-button>
                <el-radio-button value="APPROVED">已通过</el-radio-button>
                <el-radio-button value="REJECTED">已拒绝</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <el-table :data="appeals" v-loading="appealsLoading" stripe>
            <el-table-column prop="courseName" label="课程名称" width="180" />
            <el-table-column label="缺勤日期" width="120">
              <template #default="{ row }">
                {{ row.attendanceDate }}
              </template>
            </el-table-column>
            <el-table-column label="周几" width="80">
              <template #default="{ row }">
                {{ row.dayOfWeek }}
              </template>
            </el-table-column>
            <el-table-column label="节次" width="100">
              <template #default="{ row }">
                第{{ row.startSection }}-{{ row.endSection }}节
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="申诉理由" show-overflow-tooltip min-width="200" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getAppealStatusType(row.status)">
                  {{ getAppealStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="老师回复" show-overflow-tooltip min-width="150">
              <template #default="{ row }">
                <span v-if="row.teacherComment">{{ row.teacherComment }}</span>
                <span v-else style="color: #909399">暂无回复</span>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.submittedAt) }}
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!appealsLoading && appeals.length === 0" description="暂无申诉记录" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="appealDialogVisible"
      title="提交申诉"
      width="500px"
    >
      <el-descriptions :column="1" border size="small" style="margin-bottom: 20px">
        <el-descriptions-item label="课程">
          {{ currentAttendance?.courseName }}
        </el-descriptions-item>
        <el-descriptions-item label="缺勤日期">
          {{ currentAttendance?.attendanceDate }} ({{ currentAttendance?.dayOfWeek }}) 第{{ currentAttendance?.startSection }}-{{ currentAttendance?.endSection }}节
        </el-descriptions-item>
      </el-descriptions>

      <el-form :model="appealForm" label-width="80px">
        <el-form-item label="申诉理由" required>
          <el-input
            v-model="appealForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请详细说明申诉理由，例如：病假、事假等"
            :maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="证据">
          <el-input
            v-model="appealForm.evidence"
            type="textarea"
            :rows="2"
            placeholder="请提供相关证据（可选），例如：医院证明、请假条等"
            :maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAppeal" :loading="submitting">提交申诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getStudentAttendances,
  getNotifications,
  getUnreadNotifications,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
  getStudentAppeals,
  createAppeal as createAppealApi
} from '@/api/attendance'

const activeTab = ref('records')
const attendances = ref([])
const notifications = ref([])
const appeals = ref([])
const loading = ref(false)
const notificationsLoading = ref(false)
const appealsLoading = ref(false)
const unreadCount = ref(0)

const filterStatus = ref('')
const appealStatusFilter = ref('')

const appealDialogVisible = ref(false)
const currentAttendance = ref(null)
const appealForm = ref({
  reason: '',
  evidence: ''
})
const submitting = ref(false)

const getStatusType = (status) => {
  const map = {
    PRESENT: 'success',
    ABSENT: 'danger',
    LATE: 'warning',
    EXCUSED: 'info'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    PRESENT: '出勤',
    ABSENT: '缺勤',
    LATE: '迟到',
    EXCUSED: '事假'
  }
  return map[status] || status
}

const getAppealStatusType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || ''
}

const getAppealStatusText = (status) => {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

const getAppealTagType = (status) => {
  const map = {
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const fetchAttendances = async () => {
  loading.value = true
  try {
    const res = await getStudentAttendances()
    let data = res.data

    if (filterStatus.value) {
      data = data.filter(a => a.status === filterStatus.value)
    }

    attendances.value = data.sort((a, b) => {
      return new Date(b.attendanceDate) - new Date(a.attendanceDate)
    })
  } catch (error) {
    console.error(error)
    ElMessage.error('获取考勤记录失败')
  } finally {
    loading.value = false
  }
}

const fetchNotifications = async () => {
  notificationsLoading.value = true
  try {
    const res = await getNotifications()
    notifications.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取通知失败')
  } finally {
    notificationsLoading.value = false
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchAppeals = async () => {
  appealsLoading.value = true
  try {
    const res = await getStudentAppeals()
    let data = res.data

    if (appealStatusFilter.value) {
      data = data.filter(a => a.status === appealStatusFilter.value)
    }

    appeals.value = data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取申诉记录失败')
  } finally {
    appealsLoading.value = false
  }
}

const handleTabChange = (tab) => {
  if (tab === 'notifications') {
    fetchNotifications()
    fetchUnreadCount()
  } else if (tab === 'appeals') {
    fetchAppeals()
  }
}

const createAppeal = (row) => {
  currentAttendance.value = row
  appealForm.value.reason = ''
  appealForm.value.evidence = ''
  appealDialogVisible.value = true
}

const submitAppeal = async () => {
  if (!currentAttendance.value) return

  if (!appealForm.value.reason.trim()) {
    ElMessage.warning('请填写申诉理由')
    return
  }

  submitting.value = true
  try {
    await createAppealApi({
      attendanceId: currentAttendance.value.id,
      reason: appealForm.value.reason,
      evidence: appealForm.value.evidence
    })

    ElMessage.success('申诉提交成功，请等待老师审核')
    appealDialogVisible.value = false
    fetchAttendances()
    fetchAppeals()
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const markAsRead = async (row) => {
  try {
    await markNotificationRead(row.id)
    ElMessage.success('已标记为已读')
    fetchNotifications()
    fetchUnreadCount()
  } catch (error) {
    console.error(error)
    ElMessage.error('操作失败')
  }
}

const markAllRead = async () => {
  try {
    await ElMessageBox.confirm('确定要将所有通知标记为已读吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })

    await markAllNotificationsRead()
    ElMessage.success('已全部标记为已读')
    fetchNotifications()
    fetchUnreadCount()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  fetchAttendances()
  fetchUnreadCount()
})
</script>

<style scoped>
.student-attendance {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.unread-item {
  font-weight: 600;
}

:deep(.el-tabs__content) {
  padding-top: 16px;
}
</style>
