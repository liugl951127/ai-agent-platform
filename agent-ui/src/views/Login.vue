<template>
  <div class="login">
    <div class="bg-decor"></div>
    <el-card class="box" shadow="hover">
      <div class="brand">
        <span class="logo">🤖</span>
        <h2>Agent 平台</h2>
        <p class="subtitle">分布式 AI Agent 协作平台</p>
      </div>
      <el-form :model="form" size="large" @keyup.enter="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width:100%" @click="submit">登 录</el-button>
      </el-form>
      <div class="footer">
        <span>默认账号 admin / 123456</span>
        <a href="https://github.com/liugl951127/ai-agent-platform" target="_blank">
          <el-icon><Link /></el-icon> GitHub
        </a>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Link } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const form = reactive({ username: 'admin', password: '123456' })
const loading = ref(false)
const router = useRouter()
const user = useUserStore()

const submit = async () => {
  if (!form.username || !form.password) { ElMessage.warning('请输入账号密码'); return }
  loading.value = true
  try {
    await user.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) { /* 拦截器已提示 */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.login {
  height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative; overflow: hidden;
}
.bg-decor {
  position: absolute; width: 200%; height: 200%; top: -50%; left: -50%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 1px, transparent 1px);
  background-size: 30px 30px; animation: float 30s linear infinite;
}
@keyframes float { from { transform: translate(0,0) } to { transform: translate(30px,30px) } }
.box { width: 380px; padding: 20px; position: relative; z-index: 1; }
.brand { text-align: center; margin-bottom: 24px; }
.logo { font-size: 48px; }
.brand h2 { margin: 8px 0 4px; font-size: 22px; }
.subtitle { color: #999; font-size: 12px; margin: 0; }
.footer {
  display: flex; justify-content: space-between; margin-top: 16px;
  font-size: 12px; color: #999;
}
.footer a { color: var(--el-color-primary); text-decoration: none; }
</style>
