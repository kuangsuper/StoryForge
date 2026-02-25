import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/utils/storage'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const { code, message, data } = res.data
    if (code !== 200) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(res.data)
    }
    return data
  },
  (err) => {
    if (err.response?.status === 401) {
      clearAuth()
      window.location.href = '/login'
      return Promise.reject(err)
    }
    ElMessage.error(err.response?.data?.message || '网络错误')
    return Promise.reject(err)
  }
)

export default request
