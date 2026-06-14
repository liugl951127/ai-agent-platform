<template>
  <div>
    <el-card>
      <h2>📚 知识库 (RAG)</h2>
      <p class="sub">文档 → 语义切分 → Embedding → 向量召回 → 重排 → Prompt. 支持本地 hash embedding 和 OpenAI 兼容 API.</p>

      <el-tabs v-model="tab">
        <!-- 知识库列表 -->
        <el-tab-pane label="知识库" name="list">
          <div class="toolbar">
            <el-button type="primary" @click="addKb = true"><el-icon><Plus /></el-icon>新建知识库</el-button>
            <el-input v-model="searchKb" placeholder="搜索..." style="width:200px;margin-left:8px" clearable />
          </div>
          <el-table :data="filtered" stripe v-loading="loading">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="docCount" label="文档数" width="100" />
            <el-table-column prop="chunkCount" label="Chunk 数" width="100" />
            <el-table-column prop="embedding" label="Embedding" width="140" />
            <el-table-column prop="created" label="创建时间" width="170" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button text type="primary" @click="openKb(row)">文档</el-button>
                <el-button text type="success" @click="searchDialog = true; kb = row">检索测试</el-button>
                <el-button text type="danger" @click="del(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 文档管理 -->
        <el-tab-pane label="文档" name="docs">
          <el-upload :action="`/api/knowledge/upload`" :on-success="uploaded" :show-file-list="false">
            <el-button type="primary"><el-icon><Upload /></el-icon>上传文档</el-button>
          </el-upload>
          <p class="sub">支持 .txt / .md / .pdf / .docx. 自动切分 + Embedding.</p>
          <el-table :data="docs" stripe style="margin-top:12px">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="文件名" />
            <el-table-column prop="kbId" label="知识库" width="100" />
            <el-table-column prop="size" label="大小" width="100" />
            <el-table-column prop="chunks" label="Chunks" width="80" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status==='ready' ? 'success' : 'warning'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新建知识库 -->
    <el-dialog v-model="addKb" title="新建知识库" width="500px">
      <el-form :model="kbForm" label-width="100px">
        <el-form-item label="名称"><el-input v-model="kbForm.name" /></el-form-item>
        <el-form-item label="Embedding">
          <el-select v-model="kbForm.embedding">
            <el-option label="Hash (本地, 256 维)" value="hash" />
            <el-option label="OpenAI text-embedding-3-small" value="openai-3-small" />
            <el-option label="OpenAI text-embedding-3-large" value="openai-3-large" />
            <el-option label="BGE (本地)" value="bge-small-zh" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="kbForm.desc" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addKb = false">取消</el-button>
        <el-button type="primary" @click="createKb">创建</el-button>
      </template>
    </el-dialog>

    <!-- 检索测试 -->
    <el-dialog v-model="searchDialog" title="RAG 检索测试" width="700px">
      <el-input v-model="query" type="textarea" :rows="2" placeholder="输入查询..." />
      <el-button type="primary" :loading="searching" @click="doSearch" style="margin-top:8px">检索</el-button>
      <div v-if="results.length" style="margin-top:12px">
        <h4>命中 {{ results.length }} 段:</h4>
        <el-card v-for="(r, i) in results" :key="i" class="result-card" shadow="never">
          <div class="result-head">
            <el-tag size="small">#{{ i+1 }}</el-tag>
            <span>score: {{ r.score.toFixed(3) }}</span>
          </div>
          <div class="result-body">{{ r.content }}</div>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import http from '@/api/request'

const tab = ref('list')
const loading = ref(false)
const kbs = ref([])
const docs = ref([])
const searchKb = ref('')
const addKb = ref(false)
const kbForm = ref({ name: '', embedding: 'hash', desc: '' })
const searchDialog = ref(false)
const query = ref('')
const results = ref([])
const searching = ref(false)
const kb = ref(null)

const filtered = computed(() =>
  kbs.value.filter(k => !searchKb.value || k.name?.includes(searchKb.value))
)

const load = async () => {
  loading.value = true
  try { const r = await http.get('/knowledge/list'); kbs.value = r.data || [] } catch (e) {} finally { loading.value = false }
}

const loadDocs = async () => {
  try { const r = await http.get('/knowledge/docs'); docs.value = r.data || [] } catch (e) {}
}

const createKb = async () => {
  try { await http.post('/knowledge/save', kbForm.value); ElMessage.success('已创建'); addKb.value = false; load() } catch (e) {}
}

const openKb = async (row) => { tab.value = 'docs'; /* 加载该 kb 文档 */ }

const del = async (row) => {
  await ElMessageBox.confirm(`删除知识库 "${row.name}"?`, '确认', { type: 'warning' })
  try { await http.delete(`/knowledge/delete/${row.id}`); ElMessage.success('已删除'); load() } catch (e) {}
}

const uploaded = (res) => { ElMessage.success('上传成功'); loadDocs() }

const doSearch = async () => {
  if (!query.value) return
  searching.value = true
  try {
    const r = await http.post('/knowledge/search', { kbId: kb.value?.id, q: query.value, topK: 5 })
    results.value = r.data || []
  } catch (e) {} finally { searching.value = false }
}

onMounted(() => { load(); loadDocs() })
</script>

<style scoped>
h2 { margin: 0 0 4px; }
.sub { color: #666; font-size: 13px; margin: 0 0 16px; }
.toolbar { display: flex; align-items: center; margin-bottom: 12px; }
.result-card { margin-bottom: 8px; }
.result-head { display: flex; gap: 8px; align-items: center; color: #666; font-size: 12px; margin-bottom: 4px; }
.result-body { font-size: 13px; line-height: 1.6; max-height: 120px; overflow: auto; }
</style>
