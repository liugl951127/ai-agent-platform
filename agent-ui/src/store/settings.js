import { defineStore } from 'pinia'

/**
 * 全局设置 (持久化到 localStorage)
 */
export const useSettingsStore = defineStore('settings', {
  state: () => ({
    darkMode: localStorage.getItem('darkMode') === 'true',
    enableStream: localStorage.getItem('enableStream') !== 'false', // 默认开
    primaryColor: localStorage.getItem('primaryColor') || '#409EFF',
    showWelcome: localStorage.getItem('showWelcome') !== 'false'
  }),
  actions: {
    setDark(v) {
      this.darkMode = v
      localStorage.setItem('darkMode', v)
    },
    setStream(v) {
      this.enableStream = v
      localStorage.setItem('enableStream', v)
    },
    setPrimaryColor(c) {
      this.primaryColor = c
      localStorage.setItem('primaryColor', c)
      document.documentElement.style.setProperty('--el-color-primary', c)
    }
  }
})
