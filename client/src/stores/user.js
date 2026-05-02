import { defineStore } from 'pinia'
import { login, getCurrentUser } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token') || null
  }),

  getters: {
    isTeacher: (state) => state.user?.role === 'TEACHER',
    isStudent: (state) => state.user?.role === 'STUDENT',
    isLoggedIn: (state) => !!state.token
  },

  actions: {
    async login(username, password) {
      try {
        const res = await login({ username, password })
        this.token = res.data.token
        this.user = res.data
        localStorage.setItem('token', res.data.token)
        return res.data
      } catch (error) {
        throw error
      }
    },

    async getCurrentUser() {
      try {
        const res = await getCurrentUser()
        this.user = res.data
        return res.data
      } catch (error) {
        this.logout()
        throw error
      }
    },

    logout() {
      this.user = null
      this.token = null
      localStorage.removeItem('token')
      router.push('/login')
    }
  }
})