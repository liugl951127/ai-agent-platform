import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const http = axios.create({ baseURL: '/api' })
http.interceptors.request.use(c => {
  const t = localStorage.getItem('token')
  if (t) c.headers.Authorization = 'Bearer ' + t
  return c
})
http.interceptors.response.use(r => {
  if (r.data.code !== 200) {
    ElMessage.error(r.data.message)
    return Promise.reject(r.data)
  }
  return r.data
}, e => {
  if (e.response?.status === 401) { router.push('/login') }
  ElMessage.error('网络异常')
  return Promise.reject(e)
})
export default http
