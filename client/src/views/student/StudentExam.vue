<template>
  <div class="exam-container">
    <el-dialog v-model="submitConfirmVisible" title="确认提交" width="400px">
      <div style="margin-bottom: 16px">
        <p>您已完成 <strong style="color: #67c23a">{{ answeredCount }}</strong> 题，还有 <strong style="color: #f56c6c">{{ unansweredCount }}</strong> 题未答。</p>
        <p v-if="unansweredCount > 0" style="color: #e6a23c; margin-top: 8px">确定要提交试卷吗？提交后将无法修改答案。</p>
        <p v-else style="color: #67c23a; margin-top: 8px">您已完成所有题目，确定要提交吗？</p>
      </div>
      <template #footer>
        <el-button @click="submitConfirmVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSubmit" :loading="submitting">确认提交</el-button>
      </template>
    </el-dialog>

    <div class="exam-header">
      <div class="exam-title">
        <h2>{{ exam.title }}</h2>
        <span class="exam-info">总分：{{ exam.totalScore }} 分 | 时长：{{ exam.duration }} 分钟</span>
      </div>
      <div class="exam-actions">
        <div class="timer" :class="{ 'time-warning': remainingMinutes <= 10, 'time-danger': remainingMinutes <= 5 }">
          <el-icon><Timer /></el-icon>
          <span>{{ formatTime(remainingSeconds) }}</span>
        </div>
        <el-button type="danger" @click="showSubmitConfirm">
          <el-icon><Check /></el-icon>
          提交试卷
        </el-button>
      </div>
    </div>

    <div class="exam-body">
      <div class="exam-sidebar">
        <div class="sidebar-title">
          <el-icon><List /></el-icon>
          题目导航
        </div>
        <div class="question-summary">
          <span class="summary-item">
            <span class="dot answered"></span>
            已答 ({{ answeredCount }})
          </span>
          <span class="summary-item">
            <span class="dot unanswered"></span>
            未答 ({{ unansweredCount }})
          </span>
          <span class="summary-item">
            <span class="dot current"></span>
            当前
          </span>
        </div>

        <div class="question-groups" v-if="groupedQuestions">
          <div v-for="(group, type) in groupedQuestions" :key="type" class="question-group">
            <div class="group-title">{{ getTypeName(type) }} ({{ group.length }}题)</div>
            <div class="question-numbers">
              <button
                v-for="q in group"
                :key="q.sortOrder"
                class="question-number"
                :class="{
                  'answered': q.isAnswered,
                  'current': currentQuestionIndex === q.sortOrder - 1
                }"
                @click="jumpToQuestion(q.sortOrder - 1)"
              >
                {{ q.sortOrder }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="exam-content">
        <div v-if="questions && questions.length > 0 && currentQuestion" class="question-card">
          <div class="question-header">
            <span class="question-type-tag">{{ currentQuestion.typeName }}</span>
            <span class="question-score">{{ currentQuestion.score }} 分</span>
          </div>

          <div class="question-content">
            <span class="question-index">第 {{ currentQuestion.sortOrder }} 题：</span>
            <div class="question-text">{{ currentQuestion.content }}</div>
          </div>

          <div v-if="currentQuestion.options" class="question-options">
            <div v-if="currentQuestion.type === 'SINGLE_CHOICE'">
              <el-radio-group v-model="currentAnswer" @change="handleSingleChoiceChange">
                <el-radio
                  v-for="(option, index) in parseOptions(currentQuestion.options)"
                  :key="index"
                  :label="String.fromCharCode(65 + index)"
                  class="option-item"
                >
                  <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
                  <span class="option-text">{{ option }}</span>
                </el-radio>
              </el-radio-group>
            </div>

            <div v-else-if="currentQuestion.type === 'MULTIPLE_CHOICE'">
              <el-checkbox-group v-model="currentAnswerArray" @change="handleMultipleChoiceChange">
                <el-checkbox
                  v-for="(option, index) in parseOptions(currentQuestion.options)"
                  :key="index"
                  :label="String.fromCharCode(65 + index)"
                  class="option-item"
                >
                  <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
                  <span class="option-text">{{ option }}</span>
                </el-checkbox>
              </el-checkbox-group>
            </div>

            <div v-else-if="currentQuestion.type === 'TRUE_FALSE'">
              <el-radio-group v-model="currentAnswer" @change="handleTrueFalseChange">
                <el-radio label="正确" class="option-item">正确</el-radio>
                <el-radio label="错误" class="option-item">错误</el-radio>
              </el-radio-group>
            </div>
          </div>

          <div v-else-if="currentQuestion.type === 'FILL_BLANK'" class="fill-blank-area">
            <el-input
              v-model="currentAnswer"
              type="textarea"
              :rows="3"
              placeholder="请输入答案"
              @change="handleFillBlankChange"
            />
          </div>

          <div v-else-if="currentQuestion.type === 'SHORT_ANSWER'" class="short-answer-area">
            <el-input
              v-model="currentAnswer"
              type="textarea"
              :rows="6"
              placeholder="请输入答案"
              @change="handleShortAnswerChange"
            />
          </div>

          <div class="question-footer">
            <el-button
              :disabled="currentQuestionIndex === 0"
              @click="prevQuestion"
            >
              <el-icon><ArrowLeft /></el-icon>
              上一题
            </el-button>
            <el-button
              type="primary"
              :disabled="currentQuestionIndex === questions.length - 1"
              @click="nextQuestion"
            >
              下一题
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>

        <el-empty v-else description="暂无题目" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Timer, List, Check, ArrowLeft, ArrowRight
} from '@element-plus/icons-vue'
import {
  getExamForStudent,
  startExam,
  saveAnswer,
  submitExam,
  getMySubmission
} from '@/api/exams'

const route = useRoute()
const router = useRouter()

const examId = computed(() => route.params.id)

const exam = ref({})
const questions = ref([])
const currentQuestionIndex = ref(0)
const submissionId = ref(null)

const answers = ref({})
const currentAnswer = ref('')
const currentAnswerArray = ref([])

const loading = ref(false)
const submitting = ref(false)
const submitConfirmVisible = ref(false)

const startedAt = ref(null)
const remainingSeconds = ref(0)
let timerInterval = null

const currentQuestion = computed(() => {
  if (questions.value && questions.value[currentQuestionIndex.value]) {
    return questions.value[currentQuestionIndex.value]
  }
  return null
})

const groupedQuestions = computed(() => {
  if (!questions.value) return {}
  const groups = {}
  questions.value.forEach((q, index) => {
    const type = q.type
    if (!groups[type]) {
      groups[type] = []
    }
    groups[type].push({
      ...q,
      isAnswered: answers.value[q.examPaperQuestionId]?.isAnswered || false
    })
  })
  return groups
})

const answeredCount = computed(() => {
  return Object.values(answers.value).filter(a => a.isAnswered).length
})

const unansweredCount = computed(() => {
  return questions.value.length - answeredCount.value
})

const remainingMinutes = computed(() => {
  return Math.floor(remainingSeconds.value / 60)
})

const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

const getTypeName = (type) => {
  const map = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  }
  return map[type] || type
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

const loadCurrentAnswer = () => {
  if (!currentQuestion.value) return
  
  const questionId = currentQuestion.value.examPaperQuestionId
  const savedAnswer = answers.value[questionId]
  
  if (currentQuestion.value.type === 'MULTIPLE_CHOICE') {
    currentAnswerArray.value = savedAnswer?.answer ? savedAnswer.answer.split('') : []
    currentAnswer.value = savedAnswer?.answer || ''
  } else {
    currentAnswer.value = savedAnswer?.answer || ''
    currentAnswerArray.value = []
  }
}

const handleSingleChoiceChange = async () => {
  await saveCurrentAnswer(currentAnswer.value)
}

const handleMultipleChoiceChange = async () => {
  const answer = currentAnswerArray.value.slice().sort().join('')
  currentAnswer.value = answer
  await saveCurrentAnswer(answer)
}

const handleTrueFalseChange = async () => {
  await saveCurrentAnswer(currentAnswer.value)
}

const handleFillBlankChange = async () => {
  await saveCurrentAnswer(currentAnswer.value)
}

const handleShortAnswerChange = async () => {
  await saveCurrentAnswer(currentAnswer.value)
}

const saveCurrentAnswer = async (answer) => {
  if (!currentQuestion.value || !submissionId.value) return
  
  const questionId = currentQuestion.value.examPaperQuestionId
  const isAnswered = answer && answer.trim() !== ''
  
  answers.value[questionId] = {
    answer: answer,
    isAnswered: isAnswered
  }
  
  try {
    await saveAnswer(submissionId.value, questionId, { studentAnswer: answer })
  } catch (error) {
    console.error('保存答案失败:', error)
  }
}

const jumpToQuestion = (index) => {
  currentQuestionIndex.value = index
  loadCurrentAnswer()
}

const prevQuestion = () => {
  if (currentQuestionIndex.value > 0) {
    currentQuestionIndex.value--
    loadCurrentAnswer()
  }
}

const nextQuestion = () => {
  if (currentQuestionIndex.value < questions.value.length - 1) {
    currentQuestionIndex.value++
    loadCurrentAnswer()
  }
}

const showSubmitConfirm = () => {
  submitConfirmVisible.value = true
}

const confirmSubmit = async () => {
  if (!submissionId.value) {
    ElMessage.error('提交记录不存在')
    return
  }
  
  submitting.value = true
  try {
    await submitExam(submissionId.value)
    submitConfirmVisible.value = false
    ElMessage.success('提交成功！')
    router.push('/student-exam')
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

const initExam = async () => {
  loading.value = true
  try {
    const examRes = await getExamForStudent(examId.value)
    exam.value = examRes.data
    questions.value = examRes.data.questions || []
    
    if (questions.value.length === 0) {
      ElMessage.warning('该考试暂无题目')
      return
    }
    
    const submissionRes = await getMySubmission(examId.value)
    if (submissionRes.data) {
      submissionId.value = submissionRes.data.id
      
      if (submissionRes.data.answers) {
        submissionRes.data.answers.forEach(a => {
          answers.value[a.examPaperQuestionId] = {
            answer: a.studentAnswer,
            isAnswered: a.isAnswered
          }
        })
      }
      
      if (submissionRes.data.status === 'SUBMITTED' || submissionRes.data.status === 'GRADED') {
        ElMessage.warning('您已提交过考试')
        router.push('/student-exam')
        return
      }
      
      if (submissionRes.data.startedAt) {
        startedAt.value = new Date(submissionRes.data.startedAt)
        const elapsedSeconds = Math.floor((Date.now() - startedAt.value.getTime()) / 1000)
        remainingSeconds.value = Math.max(0, exam.value.duration * 60 - elapsedSeconds)
      }
    } else {
      const startRes = await startExam(examId.value)
      submissionId.value = startRes.data.id
      startedAt.value = new Date()
      remainingSeconds.value = exam.value.duration * 60
    }
    
    loadCurrentAnswer()
    startTimer()
    
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '加载考试失败')
    router.push('/student-exam')
  } finally {
    loading.value = false
  }
}

const startTimer = () => {
  timerInterval = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
      if (remainingSeconds.value === 60) {
        ElMessage.warning('考试还有1分钟结束，请尽快提交！')
      }
      if (remainingSeconds.value === 0) {
        ElMessage.warning('考试时间到，自动提交试卷...')
        confirmSubmit()
      }
    }
  }, 1000)
}

const stopTimer = () => {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

onMounted(() => {
  initExam()
})

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped>
.exam-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.exam-title h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.exam-info {
  margin-left: 16px;
  color: #909399;
  font-size: 14px;
}

.exam-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.timer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 600;
  color: #606266;
  padding: 8px 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.timer.time-warning {
  color: #e6a23c;
  background: #fdf6ec;
}

.timer.time-danger {
  color: #f56c6c;
  background: #fef0f0;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.exam-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.exam-sidebar {
  width: 280px;
  background: white;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-title {
  padding: 16px;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-summary {
  display: flex;
  justify-content: space-around;
  padding: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.dot {
  width: 14px;
  height: 14px;
  border-radius: 4px;
  display: inline-block;
}

.dot.answered {
  background: #67c23a;
}

.dot.unanswered {
  background: #c0c4cc;
}

.dot.current {
  background: #409eff;
}

.question-groups {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.question-group {
  margin-bottom: 16px;
}

.group-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
  padding-left: 4px;
}

.question-numbers {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.question-number {
  width: 36px;
  height: 36px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s;
}

.question-number:hover {
  border-color: #409eff;
  color: #409eff;
}

.question-number.answered {
  background: #67c23a;
  border-color: #67c23a;
  color: white;
}

.question-number.current {
  background: #409eff;
  border-color: #409eff;
  color: white;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4);
}

.exam-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: #f0f2f5;
}

.question-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.question-type-tag {
  padding: 4px 12px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 14px;
}

.question-score {
  font-size: 14px;
  color: #909399;
}

.question-content {
  margin-bottom: 24px;
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
}

.question-index {
  font-weight: 600;
  margin-right: 8px;
}

.question-text {
  display: inline;
}

.question-options {
  margin-bottom: 24px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  transition: all 0.2s;
}

.option-item:hover {
  background: #ecf5ff;
}

.option-label {
  font-weight: 600;
  margin-right: 8px;
  color: #409eff;
}

.option-text {
  flex: 1;
  line-height: 1.6;
}

.fill-blank-area,
.short-answer-area {
  margin-bottom: 24px;
}

.question-footer {
  display: flex;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
</style>
