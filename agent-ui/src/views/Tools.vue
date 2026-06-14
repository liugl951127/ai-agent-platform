<template>
  <div class="tools-page">
    <el-card class="header-card">
      <div class="header-content">
        <div>
          <h2>🔧 工具中心</h2>
          <p class="sub">5 个内置工具, LLM 可通过 function calling 调用. 在此可手动测试和查看参数 schema.</p>
        </div>
        <el-button type="primary" @click="refresh"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
    </el-card>

    <el-row :gutter="12" style="margin-top:12px">
      <el-col v-for="t in tools" :key="t.name" :xs="24" :sm="12" :md="8" style="margin-bottom:12px">
        <el-card class="tool-card" shadow="hover" @click="open(t)">
          <div class="tool-head">
            <span class="tool-icon">{{ iconFor(t.name) }}</span>
            <div class="tool-meta">
              <div class="tool-name">{{ t.name }}</div>
              <el-tag size="small" type="info">{{ t.category }}</el-tag>
            </div>
            <el-tag size="small" :type="t.hasData ? 'success' : 'danger'">
              {{ t.hasData ? '可用' : '不可用' }}
            </el-tag>
          </div>
          <div class="tool-desc">{{ t.description }}</div>
          <details class="schema">
            <summary>参数 schema</summary>
            <pre>{{ JSON.stringify(t.parameters, null, 2) }}</pre>
          </details>
        </el-card>
      </el-col>
    </el-row>

    <!-- 测试工具 弹窗 -->
    <el-dialog v-model="dialog" :title="`测试工具: ${curTool?.name}`" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item v-for="(p, k) in params" :key="k" :label="k">
          <el-input v-model="form[k]" :placeholder="p.description || `参数 ${k}`" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="testing" @click="test">执行</el-button>
      </template>
    </el-dialog>

    <!-- 结果 -->
    <el-dialog v-model="resultDialog" title="执行结果" width="700px">
      <pre class="result">{{ result }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import http from '@/api/request'

const tools = ref([])
const dialog = ref(false)
const curTool = ref(null)
const form = ref({})
const testing = ref(false)
const resultDialog = ref(false)
const result = ref('')

const params = computed(() => {
  if (!curTool.value) return {}
  return curTool.value.parameters?.properties || {}
})

const iconFor = (n) => ({
  calculator: '🧮', http_fetch: '🌐', datetime: '🕐', json: '📋', sql_query: '🗄️'
}[n] || '🔧')

const refresh = async () => {
  try {
    const r = await http.get('/tools/list?format=json', { baseURL: '' })
    // 工具服务直连, 不走 /api 代理
    const list = r.functions || []
    tools.value = list.map(f => ({
      name: f.function.name,
      description: f.function.description,
      parameters: f.function.parameters,
      category: guessCategory(f.function.name),
      hasData: true
    }))
  } catch (e) { ElMessage.error('工具服务不可用') }
}

const guessCategory = (n) => ({
  calculator: 'math', http_fetch: 'web', datetime: 'system', json: 'data', sql_query: 'database'
}[n] || 'general')

const open = (t) => {
  curTool.value = t
  form.value = {}
  // 填入示例值
  const props = t.parameters?.properties || {}
  for (const k of Object.keys(props)) {
    if (props[k].example) form.value[k] = props[k].example
  }
  dialog.value = true
}

const test = async () => {
  testing.value = true
  try {
    const r = await http.post('/tools/invoke', { name: curTool.value.name, args: form.value }, { baseURL: '' })
    if (r.ok) {
      result.value = JSON.stringify(r.result, null, 2)
      resultDialog.value = true
    } else {
      ElMessage.error(r.error)
    }
  } catch (e) { /* 拦截器已提示 */ }
  finally { testing.value = false }
}

onMounted(refresh)
</script>

<style scoped>
.header-card { background: linear-gradient(135deg, #e3f2fd, #bbdefb); }
.header-content { display: flex; justify-content: space-between; align-items: center; }
.header-content h2 { margin: 0; }
.sub { color: #666; margin: 4px 0 0; font-size: 13px; }
.tool-card { cursor: pointer; transition: transform 0.15s; height: 200px; }
.tool-card:hover { transform: translateY(-2px); }
.tool-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.tool-icon { font-size: 28px; }
.tool-meta { flex: 1; }
.tool-name { font-weight: 600; font-size: 15px; }
.tool-desc { font-size: 12px; color: #666; line-height: 1.5; max-height: 48px; overflow: hidden; }
.schema { font-size: 11px; margin-top: 8px; color: #888; }
.schema pre { background: #f8f8f8; padding: 6px; border-radius: 4px; max-height: 80px; overflow: auto; margin: 4px 0 0; }
.result { background: #1e1e1e; color: #d4d4d4; padding: 12px; border-radius: 6px; max-height: 400px; overflow: auto; }
</style>
