import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

const http = axios.create({
  baseURL: '/api',
  timeout: 60_000
})

http.interceptors.request.use(c => {
  const t = localStorage.getItem('token')
  if (t) c.headers.Authorization = 'Bearer ' + t
  return c
}, e => Promise.reject(e))

http.interceptors.response.use(r => {
  const data = r.data
  if (data && typeof data === 'object' && 'code' in data) {
    if (data.code === 200) return data
    ElMessage.error(data.message || `错误 ${data.code}`)
    return Promise.reject(data)
  }
  return data
}, e => {
  const status = e.response?.status
  const msg = e.response?.data?.message || e.message
  if (status === 401) {
    localStorage.removeItem('token')
    if (router.currentRoute.value.path !== '/login') {
      router.push('/login')
      ElMessage.warning('登录已过期, 请重新登录')
    }
  } else if (status === 403) {
    ElMessage.error('没有权限')
  } else if (status === 404) {
    ElMessage.error('资源不存在')
  } else if (status >= 500) {
    ElMessage.error('服务异常: ' + msg)
  } else if (e.code === 'ECONNABORTED') {
    ElMessage.error('请求超时')
  } else if (e.message?.includes('Network')) {
    ElMessage.error('网络异常, 请检查服务是否启动')
  } else {
    ElMessage.error(msg || '请求失败')
  }
  return Promise.reject(e)
})

export default http
