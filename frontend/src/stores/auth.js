import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchMe } from '../api/user'

const TOKEN_KEY = 'railway.token'
const USER_KEY = 'railway.user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const user = ref(null)
  const isLoggedIn = computed(() => Boolean(token.value))

  function restore() {
    token.value = localStorage.getItem(TOKEN_KEY) || ''
    const raw = localStorage.getItem(USER_KEY)
    user.value = raw ? JSON.parse(raw) : null
  }

  function setSession(payload) {
    token.value = payload.token
    user.value = payload.user
    localStorage.setItem(TOKEN_KEY, payload.token)
    localStorage.setItem(USER_KEY, JSON.stringify(payload.user))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  async function refreshMe() {
    if (!token.value) return
    user.value = await fetchMe()
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  return { token, user, isLoggedIn, restore, setSession, logout, refreshMe }
})
