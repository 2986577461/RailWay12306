import axios from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

function isIdKey(key) {
  return key === 'id' || key.endsWith('Id') || key.endsWith('Ids')
}

function stringifyId(value) {
  if (value == null || value === '') return value
  return String(value)
}

function reviveIds(key, value) {
  if (!isIdKey(key) || value == null || value === '') return value
  if (Array.isArray(value)) return value.map((item) => (item == null ? item : stringifyId(item)))
  return stringifyId(value)
}

/** Keep Java Long / snowflake IDs as strings so they don't overflow JS Number. */
export function parseApiJson(data) {
  if (typeof data !== 'string') return data
  const text = data.trim()
  if (!text || !(text.startsWith('{') || text.startsWith('['))) return data
  const withLongs = text.replace(
    /"(?:\\.|[^"\\])*"|-?\d{16,}/g,
    (token) => (token.startsWith('"') ? token : `"${token}"`)
  )
  return JSON.parse(withLongs, reviveIds)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  transformResponse: [parseApiJson]
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && body.success === false) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body?.data
  },
  (error) => {
    if (error.response?.status === 401) {
      const auth = useAuthStore()
      auth.logout()
      if (router.currentRoute.value.name !== 'login') {
        router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      }
      return Promise.reject(new Error(error.response.data?.message || '未登录或登录已过期'))
    }
    const message = error.response?.data?.message || error.message || '网络错误'
    return Promise.reject(new Error(message))
  }
)

export default http
