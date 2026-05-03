<template>
  <div class="question-bank-management">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="题目列表" name="list">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>题库管理</span>
              <div class="header-actions">
                <el-select v-model="filterCourseId" placeholder="选择课程" clearable style="width: 180px; margin-right: 10px">
                  <el-option
                    v-for="course in courses"
                    :key="course.id"
                    :label="course.courseName"
                    :value="course.id"
                  />
                </el-select>
                <el-select v-model="filterType" placeholder="题目类型" clearable style="width: 140px; margin-right: 10px">
                  <el-option
                    v-for="type in questionTypes"
                    :key="type.value"
                    :label="type.label"
                    :value="type.value"
                  />
                </el-select>
                <el-upload
                  ref="importUpload"
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  :limit="1"
                  accept=".doc,.docx,.pdf"
                  style="display: inline-block; margin-right: 10px"
                >
                  <el-button type="success">
                    <el-icon><Upload /></el-icon>
                    导入题目
                  </el-button>
                </el-upload>
                <el-button type="primary" @click="openCreateDialog">
                  <el-icon><Plus /></el-icon>
                  新建题目
                </el-button>
              </div>
            </div>
          </template>

          <el-table :data="filteredQuestions" v-loading="loading" stripe>
            <el-table-column prop="typeName" label="类型" width="100" />
            <el-table-column prop="content" label="题目内容" min-width="300" show-overflow-tooltip />
            <el-table-column prop="courseName" label="所属课程" width="120" />
            <el-table-column prop="score" label="分值" width="80" />
            <el-table-column prop="difficulty" label="难度" width="80">
              <template #default="{ row }">
                <el-tag :type="getDifficultyType(row.difficulty)" size="small">
                  {{ row.difficulty || '未设置' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="tag" label="标签" width="100" show-overflow-tooltip />
            <el-table-column label="公开" width="60">
              <template #default="{ row }">
                <el-tag v-if="row.isPublic" type="success" size="small">是</el-tag>
                <el-tag v-else type="info" size="small">否</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="viewQuestion(row)">查看</el-button>
                <el-button type="warning" link @click="editQuestion(row)">编辑</el-button>
                <el-button type="danger" link @click="deleteQuestion(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && questions.length === 0" description="暂无题目" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="questionDialogVisible"
      :title="isEditing ? '编辑题目' : '新建题目'"
      width="800px"
    >
      <el-form :model="questionForm" label-width="100px" :rules="questionRules" ref="questionFormRef">
        <el-form-item label="所属课程">
          <el-select v-model="questionForm.courseId" placeholder="选择课程（可选）" style="width: 100%">
            <el-option
              v-for="course in courses"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="题目类型" prop="type">
          <el-select v-model="questionForm.type" placeholder="选择题目类型" style="width: 100%">
            <el-option
              v-for="type in questionTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="题目内容" prop="content">
          <el-input
            v-model="questionForm.content"
            type="textarea"
            :rows="3"
            placeholder="请输入题目内容"
          />
        </el-form-item>
        <el-form-item label="选项" v-if="showOptions">
          <el-input
            v-model="questionForm.options"
            type="textarea"
            :rows="3"
            placeholder="请输入选项，多个选项用换行分隔&#10;例如：&#10;A. 选项1&#10;B. 选项2&#10;C. 选项3&#10;D. 选项4"
          />
          <div style="margin-top: 8px; color: #909399; font-size: 12px">
            提示：每个选项占一行，格式如 "A. 选项内容"
          </div>
        </el-form-item>
        <el-form-item label="正确答案" prop="answer">
          <el-input
            v-model="questionForm.answer"
            type="textarea"
            :rows="2"
            placeholder="请输入正确答案"
          />
          <div style="margin-top: 8px; color: #909399; font-size: 12px" v-if="showOptions">
            提示：单选题填如 "A"，多选题填如 "AB"，判断题填如 "正确" 或 "错误"
          </div>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分值">
              <el-input-number v-model="questionForm.score" :min="0.5" :max="100" :step="0.5" />
              <span style="margin-left: 10px; color: #909399">分</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度">
              <el-select v-model="questionForm.difficulty" placeholder="选择难度" style="width: 100%">
                <el-option label="简单" value="简单" />
                <el-option label="中等" value="中等" />
                <el-option label="困难" value="困难" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标签">
          <el-input v-model="questionForm.tag" placeholder="输入标签，如：第一章、重点、易错点等" />
        </el-form-item>
        <el-form-item label="题目解析">
          <el-input
            v-model="questionForm.explanation"
            type="textarea"
            :rows="2"
            placeholder="请输入题目解析（可选）"
          />
        </el-form-item>
        <el-form-item label="设为公开">
          <el-switch v-model="questionForm.isPublic" />
          <span style="margin-left: 10px; color: #909399">公开后其他教师也可以查看使用</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitQuestion" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="题目详情"
      width="700px"
    >
      <el-descriptions :column="1" border v-if="currentQuestion">
        <el-descriptions-item label="题目类型">
          <el-tag>{{ currentQuestion.typeName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属课程">
          {{ currentQuestion.courseName || '未关联课程' }}
        </el-descriptions-item>
        <el-descriptions-item label="题目内容">
          <div style="white-space: pre-wrap; word-break: break-all;">
            {{ currentQuestion.content }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="选项" v-if="currentQuestion.options">
          <div style="white-space: pre-wrap; word-break: break-all;">
            {{ currentQuestion.options }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="正确答案">
          <div style="white-space: pre-wrap; word-break: break-all;">
            {{ currentQuestion.answer }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="分值">
          {{ currentQuestion.score }} 分
        </el-descriptions-item>
        <el-descriptions-item label="难度">
          <el-tag :type="getDifficultyType(currentQuestion.difficulty)">
            {{ currentQuestion.difficulty || '未设置' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="标签" v-if="currentQuestion.tag">
          {{ currentQuestion.tag }}
        </el-descriptions-item>
        <el-descriptions-item label="题目解析" v-if="currentQuestion.explanation">
          <div style="white-space: pre-wrap; word-break: break-all;">
            {{ currentQuestion.explanation }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="公开状态">
          <el-tag :type="currentQuestion.isPublic ? 'success' : 'info'">
            {{ currentQuestion.isPublic ? '公开' : '私有' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog
      v-model="importDialogVisible"
      title="导入题目"
      width="600px"
    >
      <el-alert
        title="导入说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <template #default>
          <p>支持导入 Word (.doc, .docx) 和 PDF 文件格式</p>
          <p>文件格式要求：</p>
          <p>1. 每道题目以数字加句号开头，如 "1.", "2."</p>
          <p>2. 选择题选项以 A. B. C. D. 开头</p>
          <p>3. 答案以 "答案：" 或 "参考答案：" 开头</p>
        </template>
      </el-alert>

      <el-upload
        drag
        :auto-upload="false"
        :on-change="handleImportFileChange"
        :limit="1"
        accept=".doc,.docx,.pdf"
        :file-list="importFileList"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">只能上传 doc/docx/pdf 文件</div>
        </template>
      </el-upload>

      <div v-if="importPreview.length > 0" style="margin-top: 20px">
        <h4 style="margin-bottom: 10px">预览解析结果（{{ importPreview.length }} 道题目）：</h4>
        <div style="max-height: 300px; overflow-y: auto;">
          <el-card
            v-for="(q, index) in importPreview"
            :key="index"
            shadow="never"
            style="margin-bottom: 10px"
          >
            <div style="font-weight: 500; margin-bottom: 8px">
              {{ index + 1 }}. [{{ getQuestionTypeLabel(q.type) }}] {{ q.content?.substring(0, 50) }}{{ q.content?.length > 50 ? '...' : '' }}
            </div>
            <div style="color: #909399; font-size: 12px">
              答案: {{ q.answer?.substring(0, 30) }}{{ q.answer?.length > 30 ? '...' : '' }}
            </div>
          </el-card>
        </div>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmImport"
          :loading="importing"
          :disabled="importPreview.length === 0"
        >
          确认导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, UploadFilled } from '@element-plus/icons-vue'
import { getTeacherCourses } from '@/api/courses'
import {
  getTeacherQuestions,
  getQuestionById,
  createQuestion,
  updateQuestion,
  deleteQuestion as deleteQuestionApi,
  getQuestionTypes,
  batchCreateQuestions
} from '@/api/questions'

const activeTab = ref('list')
const courses = ref([])
const questions = ref([])
const questionTypes = ref([])
const loading = ref(false)
const submitting = ref(false)

const filterCourseId = ref(null)
const filterType = ref(null)

const filteredQuestions = computed(() => {
  let result = questions.value
  if (filterType.value) {
    result = result.filter(q => q.type === filterType.value)
  }
  return result
})

const questionDialogVisible = ref(false)
const isEditing = ref(false)
const currentQuestion = ref(null)
const questionFormRef = ref(null)
const questionForm = ref({
  courseId: null,
  type: null,
  content: '',
  options: '',
  answer: '',
  score: 1.0,
  difficulty: '',
  tag: '',
  explanation: '',
  isPublic: false
})

const questionRules = {
  type: [{ required: true, message: '请选择题目类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入正确答案', trigger: 'blur' }]
}

const detailDialogVisible = ref(false)

const importDialogVisible = ref(false)
const importFileList = ref([])
const importPreview = ref([])
const importing = ref(false)
const importFile = ref(null)

const showOptions = computed(() => {
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(questionForm.value.type)
})

const getDifficultyType = (difficulty) => {
  const map = {
    '简单': 'success',
    '中等': 'warning',
    '困难': 'danger'
  }
  return map[difficulty] || 'info'
}

const getQuestionTypeLabel = (type) => {
  const map = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'FILL_BLANK': '填空题',
    'SHORT_ANSWER': '简答题'
  }
  return map[type] || type
}

const fetchCourses = async () => {
  try {
    const res = await getTeacherCourses()
    courses.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchQuestionTypes = async () => {
  try {
    const res = await getQuestionTypes()
    questionTypes.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchQuestions = async () => {
  loading.value = true
  try {
    const res = await getTeacherQuestions(filterCourseId.value)
    questions.value = res.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取题目列表失败')
  } finally {
    loading.value = false
  }
}

watch(filterCourseId, () => {
  fetchQuestions()
})

const openCreateDialog = () => {
  isEditing.value = false
  currentQuestion.value = null
  questionForm.value = {
    courseId: courses.value.length > 0 ? courses.value[0].id : null,
    type: null,
    content: '',
    options: '',
    answer: '',
    score: 1.0,
    difficulty: '',
    tag: '',
    explanation: '',
    isPublic: false
  }
  questionDialogVisible.value = true
}

const editQuestion = (row) => {
  isEditing.value = true
  currentQuestion.value = row
  questionForm.value = {
    courseId: row.courseId || null,
    type: row.type,
    content: row.content,
    options: row.options || '',
    answer: row.answer,
    score: row.score,
    difficulty: row.difficulty || '',
    tag: row.tag || '',
    explanation: row.explanation || '',
    isPublic: row.isPublic || false
  }
  questionDialogVisible.value = true
}

const viewQuestion = async (row) => {
  try {
    const res = await getQuestionById(row.id)
    currentQuestion.value = res.data
    detailDialogVisible.value = true
  } catch (error) {
    console.error(error)
    ElMessage.error('获取题目详情失败')
  }
}

const submitQuestion = async () => {
  if (!questionFormRef.value) return

  await questionFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const data = {
        courseId: questionForm.value.courseId,
        type: questionForm.value.type,
        content: questionForm.value.content,
        options: questionForm.value.options,
        answer: questionForm.value.answer,
        score: questionForm.value.score,
        difficulty: questionForm.value.difficulty,
        tag: questionForm.value.tag,
        explanation: questionForm.value.explanation,
        isPublic: questionForm.value.isPublic
      }

      if (isEditing.value && currentQuestion.value) {
        await updateQuestion(currentQuestion.value.id, data)
        ElMessage.success('更新成功')
      } else {
        await createQuestion(data)
        ElMessage.success('创建成功')
      }

      questionDialogVisible.value = false
      fetchQuestions()
    } catch (error) {
      console.error(error)
      ElMessage.error(error.response?.data?.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const deleteQuestion = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该题目吗？删除后无法恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteQuestionApi(row.id)
    ElMessage.success('删除成功')
    fetchQuestions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const handleFileChange = (file) => {
  importFile.value = file.raw
  importFileList.value = [file]
  openImportDialog()
}

const openImportDialog = () => {
  importPreview.value = []
  importDialogVisible.value = true
}

const handleImportFileChange = (file) => {
  importFile.value = file.raw
  importFileList.value = [file]
  parseFileForPreview(file.raw)
}

const parseFileForPreview = async (file) => {
  importPreview.value = []
  
  if (file.name.endsWith('.txt')) {
    const text = await readTextFile(file)
    const parsed = parseTextToQuestions(text)
    importPreview.value = parsed
  } else {
    ElMessage.warning('当前仅支持 txt 格式预览，Word/PDF 解析将在后端处理')
    importPreview.value = [{ type: 'SINGLE_CHOICE', content: '文件解析中...', answer: '（导入后可见）' }]
  }
}

const readTextFile = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target.result)
    reader.onerror = reject
    reader.readAsText(file)
  })
}

const parseTextToQuestions = (text) => {
  const questions = []
  const lines = text.split('\n')
  let currentQuestion = null

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed) continue

    const questionMatch = trimmed.match(/^(\d+)[.、]\s*(.+)$/)
    if (questionMatch) {
      if (currentQuestion) {
        questions.push(currentQuestion)
      }
      currentQuestion = {
        type: 'SHORT_ANSWER',
        content: questionMatch[2],
        options: '',
        answer: '',
        score: 1.0
      }
      continue
    }

    if (!currentQuestion) continue

    const optionMatch = trimmed.match(/^([A-Z])[.、]\s*(.+)$/)
    if (optionMatch) {
      currentQuestion.type = 'SINGLE_CHOICE'
      currentQuestion.options += (currentQuestion.options ? '\n' : '') + `${optionMatch[1]}. ${optionMatch[2]}`
      continue
    }

    const answerMatch = trimmed.match(/^(?:参考)?答案[：:]\s*(.+)$/i)
    if (answerMatch) {
      currentQuestion.answer = answerMatch[1]
      if (currentQuestion.answer.length <= 2 && /^[A-Z]+$/i.test(currentQuestion.answer)) {
        currentQuestion.type = currentQuestion.answer.length > 1 ? 'MULTIPLE_CHOICE' : 'SINGLE_CHOICE'
      } else if (/^(正确|错误|对|错|true|false|是|否)$/i.test(currentQuestion.answer)) {
        currentQuestion.type = 'TRUE_FALSE'
      }
    }
  }

  if (currentQuestion) {
    questions.push(currentQuestion)
  }

  return questions
}

const confirmImport = async () => {
  if (importPreview.value.length === 0) {
    ElMessage.warning('没有可导入的题目')
    return
  }

  importing.value = true
  try {
    if (importFile.value && importFile.value.name.endsWith('.txt')) {
      await batchCreateQuestions(importPreview.value)
    } else {
      ElMessage.warning('Word/PDF 文件导入需要后端解析服务，当前仅支持手动创建或 txt 文件导入')
      importing.value = false
      return
    }
    ElMessage.success(`成功导入 ${importPreview.value.length} 道题目`)
    importDialogVisible.value = false
    importFileList.value = []
    importPreview.value = []
    fetchQuestions()
  } catch (error) {
    console.error(error)
    ElMessage.error(error.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  fetchCourses()
  fetchQuestionTypes()
  fetchQuestions()
})
</script>

<style scoped>
.question-bank-management {
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

:deep(.el-tabs__content) {
  padding-top: 16px;
}
</style>
