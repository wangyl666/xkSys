<template>
  <div class="attendance-management">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="考勤记录" name="records">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>考勤记录列表</span>
              <div class="header-actions">
                <el-select v-model="filterCourseId" placeholder="选择课程" clearable style="width: 200px; margin-right: 10px">
                  <el-option
                    v-for="course in courses"
                    :key="course.id"
                    :label="course.courseName"
                    :value="course.id"
                  />
                </el-select>
                <el-date-picker
                  v-model="filterDate"
                  type="date"
                  placeholder="选择日期"
                  clearable
                  style="width: 150px; margin-right: 10px"
                />
                <el-button type="primary" @click="fetchAttendances">
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
                <el-button type="success" @click="exportStatistics" style="margin-left: 10px">
                  <el-icon><Download /></el-icon>
                  导出缺勤统计
                </el-button>
              </div>
            </div>
          </template>

          <el-table :data="attendances" v-loading="loading" stripe>
            <el-table-column prop="courseName" label="课程名称" width="180" />
            <el-table-column prop="studentName" label="学生姓名" width="120" />
            <el-table-column prop="studentUsername" label="学号" width="150" />
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
                <el-tag v-else type="info">无</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="updateStatus(row)">
                  修改状态
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && attendances.length === 0" description="暂无考勤记录" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="申诉审核" name="appeals">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>考勤申诉列表</span>
              <el-radio-group v-model="appealStatusFilter" size="small" @change="fetchAppeals">
                <el-radio-button value="">全部</el-radio-button>
                <el-radio-button value="PENDING">待审核</el-radio-button>
                <el-radio-button value="APPROVED">已通过</el-radio-button>
                <el-radio-button value="REJECTED">已拒绝</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <el-table :data="appeals" v-loading="appealsLoading" stripe>
            <el-table-column prop="studentName" label="学生姓名" width="120" />
            <el-table-column prop="studentUsername" label="学号" width="150" />
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
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button type="success" link @click="reviewAppeal(row, true)">
                    通过
                  </el-button>
                  <el-button type="danger" link @click="reviewAppeal(row, false)">
                    拒绝
                  </el-button>
                </template>
                <template v-else>
                  <span style="color: #909399">已处理</span>
                </template>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!appealsLoading && appeals.length === 0" description="暂无申诉" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="缺勤统计" name="statistics">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>学生缺勤统计</span>
              <el-select v-model="statsCourseId" placeholder="选择课程" style="width: 200px" @change="fetchStatistics">
                <el-option
                  v-for="course in courses"
                  :key="course.id"
                  :label="course.courseName"
                  :value="course.id"
                />
              </el-select>
            </div>
          </template>

          <el-table :data="absentStatistics" v-loading="statsLoading" stripe>
            <el-table-column type="index" label="排名" width="80" />
            <el-table-column prop="studentName" label="学生姓名" width="120" />
            <el-table-column prop="studentUsername" label="学号" width="150" />
            <el-table-column label="缺勤次数" width="100">
              <template #default="{ row }">
                <el-tag :type="row.absentCount > 3 ? 'danger' : 'warning'">
                  {{ row.absentCount }}次
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="缺勤详情" min-width="300">
              <template #default="{ row }">
                <div v-for="(item, index) in row.absentDetails" :key="index" class="absent-detail">
                  <span>{{ item.attendanceDate }} ({{ item.dayOfWeek }}) 第{{ item.startSection }}-{{ item.endSection }}节</span>
                  <el-tag size="small" type="danger">缺勤</el-tag>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!statsLoading && absentStatistics.length === 0" description="暂无缺勤记录" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="updateStatusDialogVisible"
      title="修改考勤状态"
      width="400px"
    >
      <el-form :model="updateStatusForm" label-width="80px">
        <el-form-item label="学生">
          <span>{{ currentAttendance?.studentName }}</span>
        </el-form-item>
        <el-form-item label="课程">
          <span>{{ currentAttendance?.courseName }}</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="getStatusType(currentAttendance?.status)">
            {{ getStatusText(currentAttendance?.status) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新状态">
          <el-radio-group v-model="updateStatusForm.status">
            <el-radio value="PRESENT">出勤</el-radio>
            <el-radio value="ABSENT">缺勤</el-radio>
            <el-radio value="LATE">迟到</el-radio>
            <el-radio value="EXCUSED">事假</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="updateStatusForm.remark" type="textarea" :rows="3" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="updateStatusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpdateStatus" :loading="updating">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reviewDialogVisible"
      title="审核申诉"
      width="500px"
    >
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="学生">
          {{ currentAppeal?.studentName }} ({{ currentAppeal?.studentUsername }})
        </el-descriptions-item>
        <el-descriptions-item label="课程">
          {{ currentAppeal?.courseName }}
        </el-descriptions-item>
        <el-descriptions-item label="缺勤日期">
          {{ currentAppeal?.attendanceDate }} ({{ currentAppeal?.dayOfWeek }}) 第{{ currentAppeal?.startSection }}-{{ currentAppeal?.endSection }}节
        </el-descriptions-item>
        <el-descriptions-item label="申诉理由">
          {{ currentAppeal?.reason }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ currentAppeal?.submittedAt }}
        </el-descriptions-item>
      </el-descriptions>

      <el-form style="margin-top: 20px">
        <el-form-item label="审核意见">
          <el-input v-model="reviewComment" type="textarea" :rows="3" placeholder="请输入审核意见（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReview(false)">拒绝</el-button>
        <el-button type="success" @click="submitReview(true)">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeacherCourses } from '@/api/courses'
import {
  getTeacherAttendances,
  getCourseAttendances,
  updateAttendanceStatus,
  getAbsentStatistics,
  getPendingAppeals,
  getTeacherAppeals,
  reviewAppeal
} from '@/api/attendance'

const activeTab = ref('records')
const courses = ref([])
const attendances = ref([])
const appeals = ref([])
const absentStatistics = ref([])
const loading = ref(false)
const appealsLoading = ref(false)
const statsLoading = ref(false)

const filterCourseId = ref(null)
const filterDate = ref(null)
const statsCourseId = ref(null)
const appealStatusFilter = ref('')

const updateStatusDialogVisible = ref(false)
const currentAttendance = ref(null)
const updateStatusForm = ref({
  status: 'PRESENT',
  remark: ''
})
const updating = ref(false)

const reviewDialogVisible = ref(false)
const currentAppeal = ref(null)
const reviewComment = ref('')
const reviewAction = ref(null)

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

const fetchCourses = async () => {
  try {
    const res = await getTeacherCourses()
    courses.value = res.data
    if (res.data.length > 0) {
      statsCourseId.value = res.data[0].id
    }
  } catch (error) {
    console.error(error)
  }
}

const fetchAttendances = async () => {
  loading.value = true
  try {
    if (filterCourseId.value) {
      const res = await getCourseAttendances(filterCourseId.value)
      attendances.value = res.data
    } else {
      const res = await getTeacherAttendances()
      attendances.value = res.data
    }

    if (filterDate.value) {
      const dateStr = filterDate.value
      attendances.value = attendances.value.filter(a => a.attendanceDate === dateStr)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取考勤记录失败')
  } finally {
    loading.value = false
  }
}

const fetchAppeals = async () => {
  appealsLoading.value = true
  try {
    let res
    if (appealStatusFilter.value === 'PENDING') {
      res = await getPendingAppeals()
    } else {
      res = await getTeacherAppeals()
    }

    appeals.value = res.data

    if (appealStatusFilter.value && appealStatusFilter.value !== 'PENDING') {
      appeals.value = appeals.value.filter(a => a.status === appealStatusFilter.value)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取申诉列表失败')
  } finally {
    appealsLoading.value = false
  }
}

const fetchStatistics = async () => {
  if (!statsCourseId.value) {
    absentStatistics.value = []
    return
  }

  statsLoading.value = true
  try {
    const res = await getAbsentStatistics(statsCourseId.value)
    const allRecords = res.data

    const statsMap = new Map()
    allRecords.forEach(record => {
      const key = record.studentId
      if (!statsMap.has(key)) {
        statsMap.set(key, {
          studentId: record.studentId,
          studentName: record.studentName,
          studentUsername: record.studentUsername,
          absentCount: 0,
          absentDetails: []
        })
      }
      const stats = statsMap.get(key)
      stats.absentCount++
      stats.absentDetails.push({
        attendanceDate: record.attendanceDate,
        dayOfWeek: record.dayOfWeek,
        startSection: record.startSection,
        endSection: record.endSection
      })
    })

    absentStatistics.value = Array.from(statsMap.values())
      .sort((a, b) => b.absentCount - a.absentCount)
  } catch (error) {
    console.error(error)
    ElMessage.error('获取统计数据失败')
  } finally {
    statsLoading.value = false
  }
}

const handleTabChange = (tab) => {
  if (tab === 'appeals') {
    fetchAppeals()
  } else if (tab === 'statistics') {
    fetchStatistics()
  }
}

const updateStatus = (row) => {
  currentAttendance.value = row
  updateStatusForm.value.status = row.status
  updateStatusForm.value.remark = row.remark || ''
  updateStatusDialogVisible.value = true
}

const submitUpdateStatus = async () => {
  if (!currentAttendance.value) return

  updating.value = true
  try {
    await updateAttendanceStatus(
      currentAttendance.value.id,
      updateStatusForm.value.status,
      updateStatusForm.value.remark
    )
    ElMessage.success('状态修改成功')
    updateStatusDialogVisible.value = false
    fetchAttendances()
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '修改失败')
  } finally {
    updating.value = false
  }
}

const reviewAppeal = (row, approved) => {
  currentAppeal.value = row
  reviewAction.value = approved
  reviewComment.value = ''
  reviewDialogVisible.value = true
}

const submitReview = async (approved) => {
  if (!currentAppeal.value) return

  try {
    await ElMessageBox.confirm(
      `确定要${approved ? '通过' : '拒绝'}该申诉吗？`,
      '确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await reviewAppeal(
      currentAppeal.value.id,
      approved,
      reviewComment.value
    )

    ElMessage.success(approved ? '申诉已通过，缺勤记录已修改为事假' : '申诉已拒绝')
    reviewDialogVisible.value = false
    fetchAppeals()
    fetchAttendances()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error.response?.data?.message || '审核失败')
    }
  }
}

const exportStatistics = async () => {
  if (!statsCourseId.value && courses.value.length > 0) {
    statsCourseId.value = courses.value[0].id
  }

  if (!statsCourseId.value) {
    ElMessage.warning('请先选择课程')
    return
  }

  statsLoading.value = true
  try {
    const res = await getAbsentStatistics(statsCourseId.value)
    const allRecords = res.data

    const statsMap = new Map()
    allRecords.forEach(record => {
      const key = record.studentId
      if (!statsMap.has(key)) {
        statsMap.set(key, {
          studentName: record.studentName,
          studentUsername: record.studentUsername,
          absentCount: 0,
          details: []
        })
      }
      const stats = statsMap.get(key)
      stats.absentCount++
      stats.details.push(`${record.attendanceDate} (${record.dayOfWeek}) 第${record.startSection}-${record.endSection}节`)
    })

    let csvContent = '学生姓名,学号,缺勤次数,缺勤详情\n'
    statsMap.forEach((stats) => {
      csvContent += `${stats.studentName},${stats.studentUsername},${stats.absentCount},"${stats.details.join('; ')}"\n`
    })

    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `缺勤统计_${new Date().toISOString().split('T')[0]}.csv`
    link.click()
    URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('导出失败')
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  fetchCourses()
  fetchAttendances()
})
</script>

<style scoped>
.attendance-management {
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

.absent-detail {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
}

:deep(.el-tabs__content) {
  padding-top: 16px;
}
</style>
