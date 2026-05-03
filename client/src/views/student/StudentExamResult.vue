<template>
  <div class="exam-result">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>考试成绩</span>
          <el-button @click="goBack">返回列表</el-button>
        </div>
      </template>

      <div v-loading="loading" v-if="result">
        <el-descriptions :column="3" border class="result-summary">
          <el-descriptions-item label="考试标题">
            <span style="font-weight: 600">{{ result.examTitle }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="总分">
            {{ result.maxScore }} 分
          </el-descriptions-item>
          <el-descriptions-item label="我的得分">
            <span :class="getScoreClass(result.totalScore, result.maxScore)">
              {{ result.totalScore !== null ? result.totalScore : '-' }} 分
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="客观题得分">
            {{ result.objectiveScore !== null ? result.objectiveScore : '-' }} 分
          </el-descriptions-item>
          <el-descriptions-item label="主观题得分">
            {{ result.subjectiveScore !== null ? result.subjectiveScore : '-' }} 分
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ formatDateTime(result.submittedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="评分时间" :span="2" v-if="result.gradedAt">
            {{ formatDateTime(result.gradedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="教师评语" :span="3" v-if="result.teacherComment">
            <div style="white-space: pre-wrap; color: #e6a23c; background: #fdf6ec; padding: 10px; border-radius: 4px;">
              {{ result.teacherComment }}
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">答题详情</el-divider>

        <div class="answer-list" v-if="result.answers && result.answers.length > 0">
          <div
            v-for="(answer, index) in result.answers"
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
                <el-tag size="small">{{ answer.typeName }}</el-tag>
                <span class="question-score">({{ answer.score !== null ? answer.score : 0 }} / {{ answer.maxScore }} 分)</span>
              </div>
              <div class="answer-status">
                <el-tag v-if="answer.isAutoGraded" :type="answer.isCorrect ? 'success' : 'danger'" size="small">
                  {{ answer.isCorrect ? '正确' : '错误' }}
                </el-tag>
                <el-tag v-else type="warning" size="small">主观题</el-tag>
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
              </div>
            </div>

            <div class="answer-compare">
              <div class="compare-item">
                <span class="compare-label">我的答案：</span>
                <span class="compare-value">
                  {{ answer.studentAnswer || '未作答' }}
                </span>
              </div>
              <div class="compare-item">
                <span class="compare-label">正确答案：</span>
                <span class="compare-value correct-answer">
                  {{ answer.correctAnswer || '-' }}
                </span>
              </div>
            </div>

            <div v-if="answer.explanation" class="explanation">
              <span class="explanation-label">解析：</span>
              <span class="explanation-text">{{ answer.explanation }}</span>
            </div>

            <div v-if="answer.teacherComment" class="teacher-comment">
              <span class="comment-label">教师评语：</span>
              <span class="comment-text">{{ answer.teacherComment }}</span>
            </div>
          </div>
        </div>

        <el-empty v-else description="暂无答题详情" />
      </div>

      <el-empty v-else-if="!loading" description="成绩尚未公布或不存在" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyResult } from '@/api/exams'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const result = ref(null)

const examId = computed(() => route.params.id)

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
  router.push('/student-exam')
}

const fetchResult = async () => {
  loading.value = true
  try {
    const res = await getMyResult(examId.value)
    result.value = res.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchResult()
})
</script>

<style scoped>
.exam-result {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-summary {
  margin-bottom: 20px;
}

.score-excellent {
  font-weight: bold;
  color: #67c23a;
  font-size: 18px;
}

.score-good {
  font-weight: bold;
  color: #409eff;
  font-size: 18px;
}

.score-pass {
  font-weight: bold;
  color: #e6a23c;
  font-size: 18px;
}

.score-fail {
  font-weight: bold;
  color: #f56c6c;
  font-size: 18px;
}

.answer-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.answer-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.2s;
}

.answer-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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

.question-score {
  color: #909399;
  font-size: 13px;
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
  align-items: flex-start;
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

.answer-compare {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 12px;
}

.compare-item {
  display: flex;
  align-items: flex-start;
}

.compare-label {
  color: #909399;
  width: 80px;
  flex-shrink: 0;
}

.compare-value {
  flex: 1;
}

.correct-answer {
  color: #67c23a;
  font-weight: 500;
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
  padding: 10px 12px;
  background: #fdf6ec;
  border-radius: 4px;
}

.comment-label {
  color: #e6a23c;
  font-weight: 500;
}

.comment-text {
  color: #606266;
}
</style>
