<template>
  <div class="course-form">
    <el-card>
      <template #header>
        <span>{{ isEdit ? '编辑课程' : '发布新课程' }}</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 600px"
      >
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="请输入课程名称" />
        </el-form-item>

        <el-form-item label="课程描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入课程描述"
          />
        </el-form-item>

        <el-form-item label="学分" prop="credits">
          <el-input-number v-model="form.credits" :min="1" :max="10" />
        </el-form-item>

        <el-form-item label="最大选课人数" prop="maxStudents">
          <el-input-number v-model="form.maxStudents" :min="1" :max="200" />
        </el-form-item>

        <el-form-item label="上课周几" prop="dayOfWeek">
          <el-select v-model="form.dayOfWeek" placeholder="请选择周几">
            <el-option label="周一" value="MONDAY" />
            <el-option label="周二" value="TUESDAY" />
            <el-option label="周三" value="WEDNESDAY" />
            <el-option label="周四" value="THURSDAY" />
            <el-option label="周五" value="FRIDAY" />
            <el-option label="周六" value="SATURDAY" />
            <el-option label="周日" value="SUNDAY" />
          </el-select>
        </el-form-item>

        <el-form-item label="开始节次" prop="startSection">
          <el-select v-model="form.startSection" placeholder="请选择开始节次">
            <el-option v-for="i in 12" :key="i" :label="`第${i}节`" :value="i" />
          </el-select>
        </el-form-item>

        <el-form-item label="结束节次" prop="endSection">
          <el-select v-model="form.endSection" placeholder="请选择结束节次">
            <el-option v-for="i in 12" :key="i" :label="`第${i}节`" :value="i" />
          </el-select>
        </el-form-item>

        <el-form-item label="上课地点">
          <el-input v-model="form.location" placeholder="请输入上课地点" />
        </el-form-item>

        <el-form-item label="学期">
          <el-input v-model="form.semester" placeholder="例如：2024-2025学年第一学期" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            提交
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createCourse, updateCourse, getCourseById } from '@/api/courses'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  courseName: '',
  description: '',
  credits: 2,
  maxStudents: 50,
  dayOfWeek: '',
  startSection: 1,
  endSection: 2,
  location: '',
  semester: ''
})

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  credits: [{ required: true, message: '请输入学分', trigger: 'blur' }],
  maxStudents: [{ required: true, message: '请输入最大选课人数', trigger: 'blur' }],
  dayOfWeek: [{ required: true, message: '请选择上课周几', trigger: 'change' }],
  startSection: [{ required: true, message: '请选择开始节次', trigger: 'change' }],
  endSection: [{ required: true, message: '请选择结束节次', trigger: 'change' }]
}

const fetchCourse = async () => {
  if (isEdit.value) {
    loading.value = true
    try {
      const res = await getCourseById(route.params.id)
      const data = res.data
      form.courseName = data.courseName
      form.description = data.description || ''
      form.credits = data.credits
      form.maxStudents = data.maxStudents
      form.dayOfWeek = data.dayOfWeek
      form.startSection = data.startSection
      form.endSection = data.endSection
      form.location = data.location || ''
      form.semester = data.semester || ''
    } catch (error) {
      ElMessage.error('获取课程信息失败')
    } finally {
      loading.value = false
    }
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (form.startSection > form.endSection) {
    ElMessage.error('开始节次不能大于结束节次')
    return
  }

  loading.value = true
  try {
    if (isEdit.value) {
      await updateCourse(route.params.id, form)
      ElMessage.success('更新成功')
    } else {
      await createCourse(form)
      ElMessage.success('发布成功')
    }
    router.push('/courses')
  } catch (error) {
    const message = error.response?.data?.message || '操作失败'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  router.back()
}

onMounted(() => {
  fetchCourse()
})
</script>

<style scoped>
.course-form {
}
</style>