<template>
  <div>
    <el-card>
      <div class="head">
        <h2>🧠 模型管理</h2>
        <el-button type="primary" @click="addDialog = true"><el-icon><Plus /></el-icon>新增模型</el-button>
      </div>
      <p class="sub">配置 LLM 提供方, 供 agent-llm 路由使用. API Key 加密存储.</p>

      <el-table :data="models" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="provider" label="提供方" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.provider }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="modelName" label="模型" />
        <el-table-column prop="apiBase" label="API Base" show-overflow-tooltip />
        <el-table-column prop="temperature" label="温度" width="80" />
        <el-table-column prop="maxTokens" label="Max Tokens" width="120" />
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button text type="primary" @click="edit(row)">编辑</el-button>
            <el-button text type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="addDialog" :title="form.id ? '编辑' : '新增'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="提供方">
          <el-select v-model="form.provider">
            <el-option label="OpenAI" value="OPENAI" />
            <el-option label="通义千问" value="QWEN" />
            <el-option label="DeepSeek" value="DEEPSEEK" />
            <el-option label="Ollama" value="OLLAMA" />
            <el-option label="Azure" value="AZURE" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型名"><el-input v-model="form.modelName" placeholder="gpt-4o / qwen-plus / ..." /></el-form-item>
        <el-form-item label="API Base"><el-input v-model="form.apiBase" placeholder="https://api.openai.com" /></el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="温度"><el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" /></el-form-item>
        <el-form-item label="Max Tokens"><el-input-number v-model="form.maxTokens" :min="100" :max="32000" :step="100" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http from '@/api/request'

const models = ref([])
const loading = ref(false)
const addDialog = ref(false)
const saving = ref(false)
const form = ref(defaultForm())

function defaultForm() {
  return { id: null, name: '', provider: 'OPENAI', modelName: 'gpt-4o-mini', apiBase: 'https://api.openai.com', apiKey: '', temperature: 0.7, maxTokens: 2000, enabled: true }
}

const load = async () => {
  loading.value = true
  try {
    const r = await http.get('/llm/list')
    models.value = r.data || []
  } catch (e) {} finally { loading.value = false }
}

const edit = (row) => { form.value = { ...row }; addDialog.value = true }
const add = () => { form.value = defaultForm(); addDialog.value = true }

const save = async () => {
  saving.value = true
  try {
    if (form.value.id) await http.put(`/llm/update/${form.value.id}`, form.value)
    else await http.post('/llm/save', form.value)
    ElMessage.success('已保存')
    addDialog.value = false
    load()
  } catch (e) {} finally { saving.value = false }
}

const del = async (row) => {
  await ElMessageBox.confirm(`删除模型 ${row.name}?`, '确认', { type: 'warning' })
  try { await http.delete(`/llm/delete/${row.id}`); ElMessage.success('已删除'); load() } catch (e) {}
}

const toggle = async (row) => {
  try { await http.post(`/llm/toggle/${row.id}`, { enabled: row.enabled }) } catch (e) { row.enabled = !row.enabled }
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.head h2 { margin: 0; }
.sub { color: #666; font-size: 13px; margin: 8px 0 16px; }
</style>
