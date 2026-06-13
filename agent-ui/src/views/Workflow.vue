<template>
  <el-card>
    <h3>工作流 (Flowable)</h3>
    <el-alert title="已部署流程: agentChat (RAG → LLM → 工具调用 → 结束)" type="success" :closable="false"/>
    <el-form style="margin-top:16px" label-width="120">
      <el-form-item label="模型 ID"><el-input-number v-model="modelId" :min="1"/></el-form-item>
      <el-form-item label="用户输入"><el-input v-model="input"/></el-form-item>
      <el-button type="primary" @click="start" :loading="loading">启动流程</el-button>
    </el-form>
    <el-divider/>
    <h4>流程实例</h4>
    <pre>{{ piId }}</pre>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import http from '@/api/request'
import { ElMessage } from 'element-plus'

const modelId = ref(1)
const input = ref('北京今天几度?')
const piId = ref('')
const loading = ref(false)

const start = async () => {
  loading.value = true
  try {
    const r = await http.post('/workflow/start/agentChat', { modelId: modelId.value, input: input.value })
    piId.value = r.data
    ElMessage.success('已启动: ' + r.data)
  } finally { loading.value = false }
}
</script>
