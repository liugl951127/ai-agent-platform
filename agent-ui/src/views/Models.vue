<template>
  <el-card>
    <div style="display:flex;justify-content:space-between;margin-bottom:12px">
      <h3>大模型</h3>
      <el-button type="primary" @click="dlg=true">+ 新增</el-button>
    </div>
    <el-table :data="list" stripe>
      <el-table-column prop="name" label="名称"/>
      <el-table-column prop="provider" label="供应商" width="120"/>
      <el-table-column prop="modelName" label="模型"/>
      <el-table-column prop="apiBase" label="API 地址"/>
      <el-table-column prop="temperature" label="温度" width="80"/>
      <el-table-column prop="maxTokens" label="maxTokens" width="100"/>
    </el-table>
    <el-dialog v-model="dlg" title="新增模型" width="520">
      <el-form :model="form" label-width="90">
        <el-form-item label="名称"><el-input v-model="form.name"/></el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="form.provider">
            <el-option label="OpenAI" value="OPENAI"/>
            <el-option label="Ollama" value="OLLAMA"/>
            <el-option label="通义千问" value="QWEN"/>
            <el-option label="DeepSeek" value="DEEPSEEK"/>
          </el-select>
        </el-form-item>
        <el-form-item label="模型名"><el-input v-model="form.modelName" placeholder="gpt-4o-mini / qwen-plus / llama3"/></el-form-item>
        <el-form-item label="API Base"><el-input v-model="form.apiBase"/></el-form-item>
        <el-form-item label="API Key"><el-input v-model="form.apiKey" type="password"/></el-form-item>
        <el-form-item label="温度"><el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1"/></el-form-item>
        <el-form-item label="Max Tokens"><el-input-number v-model="form.maxTokens" :min="256" :max="32000"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '@/api/request'
import { ElMessage } from 'element-plus'

const list = ref([]), dlg = ref(false)
const form = reactive({ name:'', provider:'OPENAI', modelName:'', apiBase:'', apiKey:'', temperature:0.7, maxTokens:2048 })
const reload = async () => list.value = (await http.get('/llm/list')).data
onMounted(reload)
const save = async () => { await http.post('/llm/add', form); ElMessage.success('已保存'); dlg.value = false; reload() }
</script>
