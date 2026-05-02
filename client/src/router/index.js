import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'courses',
        name: 'CourseList',
        component: () => import('@/views/teacher/CourseList.vue'),
        meta: { title: '课程管理', roles: ['TEACHER'] }
      },
      {
        path: 'course/create',
        name: 'CourseCreate',
        component: () => import('@/views/teacher/CourseForm.vue'),
        meta: { title: '发布课程', roles: ['TEACHER'] }
      },
      {
        path: 'course/edit/:id',
        name: 'CourseEdit',
        component: () => import('@/views/teacher/CourseForm.vue'),
        meta: { title: '编辑课程', roles: ['TEACHER'] }
      },
      {
        path: 'select-courses',
        name: 'SelectCourses',
        component: () => import('@/views/student/SelectCourses.vue'),
        meta: { title: '选课中心', roles: ['STUDENT'] }
      },
      {
        path: 'my-courses',
        name: 'MyCourses',
        component: () => import('@/views/student/MyCourses.vue'),
        meta: { title: '我的课程', roles: ['STUDENT'] }
      },
      {
        path: 'schedule',
        name: 'Schedule',
        component: () => import('@/views/student/Schedule.vue'),
        meta: { title: '我的课表', roles: ['STUDENT'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 教务管理系统` : '教务管理系统'

  const userStore = useUserStore()
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth || to.path !== '/login') {
    if (!token) {
      next('/login')
      return
    }

    if (!userStore.user) {
      await userStore.getCurrentUser()
    }

    if (to.meta.roles && to.meta.roles.length > 0) {
      if (!to.meta.roles.includes(userStore.user?.role)) {
        ElMessage.error('无权访问该页面')
        next('/dashboard')
        return
      }
    }
  }

  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }

  next()
})

export default router