import { defineStore } from 'pinia'
import http from '@/api/request'

export const useUserStore = defineStore('user', {
  state: () => ({ token: localStorage.getItem('token') || '', user: null }),
  actions: {
    async login(form) {
      const r = await http.post('/auth/login', null, { params: form })
      this.token = r.data
      localStorage.setItem('token', this.token)
    },
    logout() {
      this.token = ''
      localStorage.removeItem('token')
      router.push('/login')
    }
  }
})
