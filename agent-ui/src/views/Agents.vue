<template>
  <div>
    <el-card>
      <div class="head">
        <h2>🤖 智能体</h2>
        <el-button type="primary" @click="addDialog = true"><el-icon><Plus /></el-icon>新建智能体</el-button>
      </div>
      <p class="sub">4 个内置角色: Researcher (联网查资料) / Writer (写报告) / Reviewer (审稿) / Coder (写代码). 配合 ReAct + 多智能体协作.</p>

      <el-row :gutter="12">
        <el-col v-for="a in agents" :key="a.id" :xs="24" :sm="12" :md="8" :lg="6" style="margin-bottom:12px">
          <el-card class="agent-card" shadow="hover">
            <div class="card-head">
              <el-avatar :size="48">{{ a.name?.[0] || 'A' }}</el-avatar>
              <div class="info">
                <div class="name">{{ a.name }}</div>
                <el-tag size="small" :type="a.enabled ? 'success' : 'info'">
                  {{ a.enabled ? '已启用' : '已停用' }}
                </el-tag>
              </div>
            </div>
            <div class="desc">{{ a.description || '智能助手' }}</div>
            <div class="stats">
              <span><el-icon><Tools /></el-icon> {{ countTools(a) }} 工具</span>
              <span><el-icon><Connection /></el-icon> {{ a.knowledgeId ? 'RAG' : '无 RAG' }}</span>
            </div>
            <div class="actions">
              <el-button text type="primary" @click="chat(a)"><el-icon><ChatDotRound /></el-icon>对话</el-button>
              <el-button text @click="edit(a)"><el-icon><Edit /></el-icon></el-button>
              <el-button text type="danger" @click="del(a)"><el-icon><Delete /></el-icon></el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 编辑 -->
    <el-dialog v-model="addDialog" :title="form.id ? '编辑' : '新建'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="System Prompt">
          <el-input v-model="form.systemPrompt" type="textarea" :rows="4" placeholder="设定 Agent 行为 / 性格 / 边界" />
        </el-form-item>
        <el-form-item label="可用工具">
          <el-select v-model="form.toolIds" multiple placeholder="选择工具">
            <el-option v-for="t in tools" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库">
          <el-select v-model="form.knowledgeId" clearable placeholder="可选, 启用 RAG">
            <el-option v-for="k in kbs" :key="k.id" :label="k.name" :value="k.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
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
import { Plus, ChatDotRound, Edit, Delete, Tools, Connection } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import http from '@/api/request'

const router = useRouter()
const agents = ref([])
const kbs = ref([])
const tools = ref(['calculator', 'http_fetch', 'datetime', 'json', 'sql_query'])
const addDialog = ref(false)
const saving = ref(false)
const form = ref({ id: null, name: '', description: '', systemPrompt: '', toolIds: [], knowledgeId: null, enabled: true })

const countTools = (a) => a.toolIds ? a.toolIds.split(',').filter(Boolean).length : 0

const load = async () => {
  try { const r = await http.get('/agent/list'); agents.value = r.data || [] } catch (e) {}
  try { const r = await http.get('/knowledge/list'); kbs.value = r.data || [] } catch (e) {}
}

const chat = (a) => { localStorage.setItem('selectedAgent', a.id); router.push('/chat') }
const edit = (a) => { form.value = { ...a, toolIds: a.toolIds ? a.toolIds.split(',') : [] }; addDialog.value = true }
const add = () => { form.value = { id: null, name: '', description: '', systemPrompt: '', toolIds: [], knowledgeId: null, enabled: true }; addDialog.value = true }

const save = async () => {
  saving.value = true
  try {
    const data = { ...form.value, toolIds: (form.value.toolIds || []).join(',') }
    if (data.id) await http.put(`/agent/update/${data.id}`, data)
    else await http.post('/agent/save', data)
    ElMessage.success('已保存'); addDialog.value = false; load()
  } catch (e) {} finally { saving.value = false }
}

const del = async (a) => {
  await ElMessageBox.confirm(`删除智能体 "${a.name}"?`, '确认', { type: 'warning' })
  try { await http.delete(`/agent/delete/${a.id}`); ElMessage.success('已删除'); load() } catch (e) {}
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.head h2 { margin: 0; }
.sub { color: #666; font-size: 13px; margin: 8px 0 16px; }
.agent-card { transition: transform 0.15s; }
.agent-card:hover { transform: translateY(-2px); }
.card-head { display: flex; gap: 10px; align-items: center; }
.info .name { font-weight: 600; font-size: 15px; }
.desc { font-size: 12px; color: #666; margin: 8px 0; min-height: 32px; }
.stats { display: flex; gap: 12px; font-size: 12px; color: #888; margin-bottom: 8px; }
.stats span { display: flex; align-items: center; gap: 4px; }
.actions { display: flex; gap: 4px; border-top: 1px solid var(--el-border-color-lighter); padding-top: 8px; }
</style>
