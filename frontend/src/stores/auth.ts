import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, setUser, getUser, clearAuth } from '@/utils/storage'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginRequest } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const user = ref<any>(getUser())

  const isLoggedIn = computed(() => !!token.value)
  const userName = computed(() => user.value?.name || '')
  const userRole = computed(() => user.value?.role || '')

  async function login(form: LoginRequest) {
    const res = await loginApi(form)
    token.value = res.token
    user.value = { id: res.userId, name: res.name, role: res.role }
    setToken(res.token)
    setUser(user.value)
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // ignore
    }
    token.value = null
    user.value = null
    clearAuth()
  }

  return { token, user, isLoggedIn, userName, userRole, login, logout }
})
