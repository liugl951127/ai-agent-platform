<template>
  <div class="login">
    <el-card class="box">
      <h2>🤖 Agent 平台</h2>
      <el-form :model="form" @keyup.enter="submit">
        <el-form-item><el-input v-model="form.username" placeholder="用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" placeholder="密码" /></el-form-item>
        <el-button type="primary" :loading="loading" style="width:100%" @click="submit">登录</el-button>
      </el-form>
      <p class="tip">默认 admin / 123456</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const form = reactive({ username: 'admin', password: '123456' })
const loading = ref(false)
const router = useRouter()
const user = useUserStore()

const submit = async () => {
  loading.value = true
  try { await user.login(form); router.push('/chat') }
  finally { loading.value = false }
}
</script>

<style scoped>
.login { height: 100vh; display:flex; justify-content:center; align-items:center;
         background: linear-gradient(135deg,#667eea,#764ba2); }
.box   { width: 380px; padding: 20px; }
.tip   { text-align:center; color:#999; font-size:12px; }
</style>
