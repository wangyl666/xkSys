<template>
  <div class="exam-grading">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button @click="goBack">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <span class="exam-title">{{ exam?.title || '考试阅卷' }}</span>
          </div>
          <div class="header-right">
            <span class="stats-info">
              已提交: {{ submittedCount }} 人 |
              已评分: {{ gradedCount }} 人 |
              未评分: {{ ungradedCount }} 人
            </span>
            <el-button
              type="success"
              :disabled="ungradedCount > 0 || submittedCount === 0 || exam?.scoresPublished"
              @click="publishAllScores"
            >
              公布所有成绩
            </el-button>
          </div>
        </div>
      </template>

      <div class="grading-container">
        <div class="student-list">
          <div class="list-header">
            <span>学生列表</span>
            <el-select v-model="filterStatus" placeholder="筛选状态" clearable size="small" style="width: 120px">
              <el-option label="全部" value="" />
              <el-option label="已评分" value="GRADED" />
              <el-option label="未评分" value="SUBMITTED" />
            </el-select>
          </div>
          <div class="student-items">
            <div
              v-for="submission in filteredSubmissions"
              :key="submission.id"
              class="student-item"
              :class="{ active: currentSubmission?.id === submission.id }"
              @click="selectStudent(submission)"
            >
              <div class="student-info">
                <span class="student-name">{{ submission.studentName }}</span>
                <span class="student-username">({{ submission.studentUsername }})</span>
              </div>
              <div class="student-status">
                <el-tag :type="submission.status === 'GRADED' ? 'success' : 'warning'" size="small">
                  {{ submission.status === 'GRADED' ? '已评分' : '未评分' }}
                </el-tag>
              </div>
              <div class="student-score" v-if="submission.totalScore !== null">
                <span :class="getScoreClass(submission.totalScore, exam?.totalScore)">
                  {{ submission.totalScore }} 分
                </span>
              </div>
            </div>
          </div>
          <el-empty v-if="submissions.length === 0" description="暂无提交的学生" :image-size="60" />
        </div>

        <div class="grading-content">
          <div v-if="currentSubmission" class="submission-detail">
            <div class="submission-header">
              <div class="student-detail">
                <el-avatar :size="40" style="background-color: #409eff">
                  {{ currentSubmission.studentName?.charAt(0) || '学' }}
                </el-avatar>
                <div class="student-info-detail">
                  <span class="name">{{ currentSubmission.studentName }}</span>
                  <span class="username">{{ currentSubmission.studentUsername }}</span>
                </div>
              </div>
              <div class="submission-stats">
                <div class="stat-item">
                  <span class="stat-label">提交时间</span>
                  <span class="stat-value">{{ formatDateTime(currentSubmission.submittedAt) }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">客观题得分</span>
                  <span class="stat-value objective">
                    {{ currentSubmission.objectiveScore !== null ? currentSubmission.objectiveScore : '-' }} 分
                  </span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">主观题得分</span>
                  <span class="stat-value subjective">
                    {{ currentSubmission.subjectiveScore !== null ? currentSubmission.subjectiveScore : '-' }} 分
                  </span>
                </div>
                <div class="stat-item total">
                  <span class="stat-label">总分</span>
                  <span class="stat-value" :class="getScoreClass(currentSubmission.totalScore, exam?.totalScore)">
                    {{ currentSubmission.totalScore !== null ? currentSubmission.totalScore : '-' }} 分
                  </span>
                </div>
              </div>
            </div>

            <el-divider content-position="left">
              答题详情
              <span style="margin-left: 8px; color: #909399; font-weight: normal">
                (共 {{ currentSubmission.answers?.length || 0 }} 题)
              </span>
            </el-divider>

            <div class="answer-list">
              <div
                v-for="(answer, index) in currentSubmission.answers"
                :key="answer.id"
                class="answer-item"
                :class="{
                  'correct': answer.isAutoGraded && answer.isCorrect,
                  'wrong': answer.isAutoGraded && answer.isCorrect === false,
                  'subjective': !answer.isAutoGraded
                }"
              >
                <div class="answer-header">
                  <div class="question-info">
                    <span class="question-number">第 {{ answer.sortOrder }} 题</span>
                    <el-tag size="small" :type="answer.isAutoGraded ? 'info' : 'warning'">
                      {{ answer.typeName }}
                    </el-tag>
                    <el-tag
                      v-if="answer.isAutoGraded"
                      size="small"
                      :type="answer.isCorrect ? 'success' : 'danger'"
                    >
                      {{ answer.isCorrect ? '正确' : '错误' }}
                    </el-tag>
                    <el-tag v-else size="small" type="warning">主观题</el-tag>
                  </div>
                  <div class="score-info">
                    <span class="score-label">得分:</span>
                    <el-input-number
                      v-if="!answer.isAutoGraded"
                      v-model="answer.score"
                      :min="0"
                      :max="answer.maxScore"
                      :step="0.5"
                      :precision="1"
                      size="small"
                      style="width: 100px"
                      @change="saveSubjectiveScore(answer)"
                    />
                    <span v-else :class="answer.isCorrect ? 'correct-score' : 'wrong-score'">
                      {{ answer.score !== null ? answer.score : 0 }}
                    </span>
                    <span class="max-score"> / {{ answer.maxScore }}</span>
                  </div>
                </div>

                <div class="question-content">
                  <div class="question-text">{{ answer.content }}</div>
                </div>

                <div v-if="answer.options" class="question-options">
                  <div
                    v-for="(option, optIndex) in parseOptions(answer.options)"
                    :key="optIndex"
                    class="option-item"
                    :class="{
                      'correct-option': isCorrectOption(answer.correctAnswer, String.fromCharCode(65 + optIndex)),
                      'selected-option': isSelectedOption(answer.studentAnswer, String.fromCharCode(65 + optIndex)) &&
                        !isCorrectOption(answer.correctAnswer, String.fromCharCode(65 + optIndex))
                    }"
                  >
                    <span class="option-label">{{ String.fromCharCode(65 + optIndex) }}.</span>
                    <span class="option-text">{{ option }}</span>
                    <el-icon v-if="isCorrectOption(answer.correctAnswer, String.fromCharCode(65 + optIndex))" class="correct-icon">
                      <Check />
                    </el-icon>
                    <el-icon
                      v-if="isSelectedOption(answer.studentAnswer, String.fromCharCode(65 + optIndex)) &&
                        !isCorrectOption(answer.correctAnswer, String.fromCharCode(65 + optIndex))"
                      class="wrong-icon"
                    >
                      <Close />
                    </el-icon>
                  </div>
                </div>

                <div v-else-if="answer.type === 'FILL_BLANK' || answer.type === 'SHORT_ANSWER'" class="fill-answer">
                  <div class="answer-row">
                    <span class="answer-label">学生答案：</span>
                    <div class="answer-content">
                      {{ answer.studentAnswer || '未作答' }}
                    </div>
                  </div>
                  <div class="answer-row">
                    <span class="answer-label">正确答案：</span>
                    <div class="answer-content correct">
                      {{ answer.correctAnswer || '-' }}
                    </div>
                  </div>
                </div>

                <div v-else class="simple-answer-compare">
                  <span class="compare-label">学生答案：</span>
                  <span :class="answer.isCorrect ? 'correct-answer' : 'wrong-answer'">
                    {{ answer.studentAnswer || '未作答' }}
                  </span>
                  <span class="compare-divider"> | </span>
                  <span class="compare-label">正确答案：</span>
                  <span class="correct-answer">{{ answer.correctAnswer || '-' }}</span>
                </div>

                <div v-if="answer.explanation" class="explanation">
                  <span class="explanation-label">解析：</span>
                  <span class="explanation-text">{{ answer.explanation }}</span>
                </div>

                <div v-if="!answer.isAutoGraded" class="teacher-comment">
                  <el-input
                    v-model="answer.teacherComment"
                    type="textarea"
                    :rows="2"
                    placeholder="输入评语（可选）"
                    @change="saveSubjectiveScore(answer)"
                  />
                </div>
              </div>
            </div>

            <div class="grading-actions">
              <el-button
                type="primary"
                :disabled="!hasUnscoredSubjective && currentSubmission.status !== 'GRADED'"
                :loading="completing"
                @click="completeGrading"
              >
                完成评分
              </el-button>
              <span class="action-hint" v-if="hasUnscoredSubjective">
                <el-icon :size="16"><Warning /></el-icon>
                存在未评分的主观题
              </span>
            </div>
          </div>

          <el-empty v-else description="请选择左侧学生查看答题详情" :image-size="100" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Check, Close, Warning
} from '@element-plus/icons-vue'
import {
  getExamById,
  getExamSubmissions,
  getSubmissionForGrading,
  gradeSubjectiveAnswer,
  completeGrading,
  publishScores
} from '@/api/exams'

const route = useRoute()
const router = useRouter()

const examId = computed(() => route.params.id)

const exam = ref(null)
const submissions = ref([])
const currentSubmission = ref(null)
const filterStatus = ref('')
const completing = ref(false)

const submittedCount = computed(() => submissions.value.length)
const gradedCount = computed(() => submissions.value.filter(s => s.status === 'GRADED').length)
const ungradedCount = computed(() => submissions.value.filter(s => s.status !== 'GRADED').length)

const filteredSubmissions = computed(() => {
  if (!filterStatus.value) {
    return submissions.value
  }
  return submissions.value.filter(s => s.status === filterStatus.value)
})

const hasUnscoredSubjective = computed(() => {
  if (!currentSubmission.value?.answers) return false
  return currentSubmission.value.answers.some(a => !a.isAutoGraded && (a.score === null || a.score === undefined))
})

const getScoreClass = (score, maxScore) => {
  if (score === null || maxScore === null || maxScore === 0) return ''
  const percentage = (score / maxScore) * 100
  if (percentage >= 90) return 'score-excellent'
  if (percentage >= 80) return 'score-good'
  if (percentage >= 60) return 'score-pass'
  return 'score-fail'
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

const parseOptions = (optionsStr) => {
  if (!optionsStr) return []
  return optionsStr.split('\n')
    .map(line => {
      const match = line.match(/^[A-Z][.、]\s*(.+)$/)
      return match ? match[1].trim() : line.trim()
    })
    .filter(line => line)
}

const isCorrectOption = (correctAnswer, option) => {
  if (!correctAnswer) return false
  return correctAnswer.toUpperCase().includes(option)
}

const isSelectedOption = (studentAnswer, option) => {
  if (!studentAnswer) return false
  return studentAnswer.toUpperCase().includes(option)
}

const goBack = () => {
  router.push('/exam-management')
}

const selectStudent = async (submission) => {
  if (currentSubmission.value?.id === submission.id) return
  
  if (submission.status === 'GRADED') {
    const res = await getSubmissionForGrading(submission.id)
    currentSubmission.value = res.data
  } else {
    currentSubmission.value = { ...submission }
  }
}

const saveSubjectiveScore = async (answer) => {
  try {
    await gradeSubjectiveAnswer(answer.id, {
      score: answer.score,
      teacherComment: answer.teacherComment
    })
    ElMessage.success('已保存评分')
  } catch (error) {
    console.error(error)
    ElMessage.error('保存失败')
  }
}

const completeGrading = async () => {
  try {
    await ElMessageBox.confirm('确定要完成评分吗？完成后将计算总分并标记为已评分。', '确认完成', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    completing.value = true
    const res = await completeGrading(currentSubmission.value.id)
    currentSubmission.value = res.data
    
    const index = submissions.value.findIndex(s => s.id === currentSubmission.value.id)
    if (index !== -1) {
      submissions.value[index] = { ...currentSubmission.value }
    }
    
    ElMessage.success('评分完成')
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  } finally {
    completing.value = false
  }
}

const publishAllScores = async () => {
  try {
    await ElMessageBox.confirm('确定要公布所有成绩吗？公布后学生可以查看自己的成绩。', '确认公布', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await publishScores(examId.value)
    ElMessage.success('成绩已公布')
    exam.value.scoresPublished = true
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error.response?.data?.message || '公布失败')
    }
  }
}

const fetchExam = async () => {
  try {
    const res = await getExamById(examId.value)
    exam.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取考试信息失败')
  }
}

const fetchSubmissions = async () => {
  try {
    const res = await getExamSubmissions(examId.value)
    submissions.value = res.data.filter(s => s.status === 'SUBMITTED' || s.status === 'GRADED')
  } catch (error) {
    console.error(error)
    ElMessage.error('获取提交列表失败')
  }
}

onMounted(() => {
  fetchExam()
  fetchSubmissions()
})
</script>

<style scoped>
.exam-grading {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.exam-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stats-info {
  color: #909399;
  font-size: 14px;
}

.grading-container {
  display: flex;
  gap: 20px;
  min-height: 600px;
}

.student-list {
  width: 280px;
  flex-shrink: 0;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
}

.student-items {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.student-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.student-item:hover {
  background: #f5f7fa;
}

.student-item.active {
  background: #ecf5ff;
  border: 1px solid #409eff;
}

.student-info {
  flex: 1;
  min-width: 0;
}

.student-name {
  font-weight: 500;
  color: #303133;
}

.student-username {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}

.student-status {
  margin-right: 8px;
}

.student-score {
  font-weight: 600;
}

.student-score .score-excellent { color: #67c23a; }
.student-score .score-good { color: #409eff; }
.student-score .score-pass { color: #e6a23c; }
.student-score .score-fail { color: #f56c6c; }

.grading-content {
  flex: 1;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow-y: auto;
  max-height: 70vh;
}

.submission-detail {
  padding: 20px;
}

.submission-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 16px;
}

.student-detail {
  display: flex;
  align-items: center;
  gap: 12px;
}

.student-info-detail {
  display: flex;
  flex-direction: column;
}

.student-info-detail .name {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.student-info-detail .username {
  color: #909399;
  font-size: 13px;
}

.submission-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.stat-value.objective { color: #409eff; }
.stat-value.subjective { color: #e6a23c; }
.stat-item.total .stat-value { font-size: 22px; }

.answer-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.answer-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.answer-item.correct {
  border-left: 4px solid #67c23a;
}

.answer-item.wrong {
  border-left: 4px solid #f56c6c;
}

.answer-item.subjective {
  border-left: 4px solid #e6a23c;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ebeef5;
}

.question-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-number {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.score-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-label {
  color: #909399;
}

.correct-score {
  color: #67c23a;
  font-weight: 600;
}

.wrong-score {
  color: #f56c6c;
  font-weight: 600;
}

.max-score {
  color: #909399;
}

.question-content {
  margin-bottom: 12px;
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
}

.question-options {
  margin-bottom: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  margin-bottom: 4px;
  background: #f5f7fa;
  border-radius: 4px;
}

.option-item.correct-option {
  background: #f0f9eb;
  border: 1px solid #67c23a;
}

.option-item.selected-option {
  background: #fef0f0;
  border: 1px solid #f56c6c;
}

.option-label {
  font-weight: 600;
  margin-right: 8px;
  color: #606266;
}

.option-text {
  flex: 1;
  line-height: 1.6;
}

.correct-icon {
  color: #67c23a;
  margin-left: 8px;
}

.wrong-icon {
  color: #f56c6c;
  margin-left: 8px;
}

.fill-answer {
  margin-bottom: 12px;
}

.answer-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.answer-label {
  color: #909399;
  width: 80px;
  flex-shrink: 0;
}

.answer-content {
  flex: 1;
  line-height: 1.6;
}

.answer-content.correct {
  color: #67c23a;
  font-weight: 500;
}

.simple-answer-compare {
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.compare-label {
  color: #909399;
}

.compare-divider {
  color: #c0c4cc;
  margin: 0 8px;
}

.correct-answer {
  color: #67c23a;
  font-weight: 500;
}

.wrong-answer {
  color: #f56c6c;
}

.explanation {
  padding: 10px 12px;
  background: #ecf5ff;
  border-radius: 4px;
  margin-bottom: 8px;
}

.explanation-label {
  color: #409eff;
  font-weight: 500;
}

.explanation-text {
  color: #606266;
}

.teacher-comment {
  margin-top: 8px;
}

.grading-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #e6a23c;
}

.score-excellent { color: #67c23a; }
.score-good { color: #409eff; }
.score-pass { color: #e6a23c; }
.score-fail { color: #f56c6c; }
</style>
