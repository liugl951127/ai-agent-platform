<template>
  <div class="chat-container">
    <el-row :gutter="12" class="chat-row">
      <!-- 左侧: 智能体列表 + 会话历史 -->
      <el-col :xs="24" :sm="6" :md="5" :lg="4" class="left-col">
        <div class="left-panel">
          <el-button type="primary" class="new-chat-btn" @click="newChat">
            <el-icon><Plus /></el-icon><span>新对话</span>
          </el-button>

          <div class="section-title">智能体</div>
          <div v-for="a in agents" :key="a.id"
               :class="['agent-item', curId===a.id && 'active']" @click="selectAgent(a)">
            <el-avatar :size="32" :src="a.avatar">{{ a.name?.[0] || 'A' }}</el-avatar>
            <span class="agent-name">{{ a.name }}</span>
            <el-tag v-if="a.toolIds" size="small" type="info" effect="plain">{{ toolCount(a) }}</el-tag>
          </div>
          <el-empty v-if="!agents.length" :image-size="60" description="暂无智能体" />

          <div class="section-title" style="margin-top:16px">历史会话</div>
          <div v-for="s in sessions" :key="s.id"
               :class="['session-item', curSession===s.id && 'active']"
               @click="loadSession(s)">
            <el-icon><ChatDotRound /></el-icon>
            <span class="session-title">{{ s.title }}</span>
            <el-icon class="del" @click.stop="delSession(s)"><Close /></el-icon>
          </div>
        </div>
      </el-col>

      <!-- 右侧: 对话区 -->
      <el-col :xs="24" :sm="18" :md="19" :lg="20" class="right-col">
        <div class="chat-panel">
          <!-- 顶部: 当前智能体信息 + 操作 -->
          <div class="chat-header">
            <div v-if="curAgent" class="agent-info">
              <el-avatar :size="36" :src="curAgent.avatar">{{ curAgent.name?.[0] }}</el-avatar>
              <div>
                <div class="agent-title">{{ curAgent.name }}</div>
                <div class="agent-sub">{{ curAgent.role || '智能助手' }}</div>
              </div>
            </div>
            <div v-else class="agent-info empty">请选择左侧智能体开始对话</div>
            <div class="header-actions">
              <el-tooltip content="清空当前对话">
                <el-button text @click="clearCur"><el-icon><Delete /></el-icon></el-button>
              </el-tooltip>
              <el-tooltip :content="settings.enableStream ? '流式输出' : '非流式输出'">
                <el-switch v-model="settings.enableStream" />
              </el-tooltip>
              <el-tooltip content="多智能体 (Supervisor)">
                <el-button text :type="multiMode ? 'primary' : ''" @click="multiMode = !multiMode">
                  <el-icon><Connection /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>

          <!-- 消息区 -->
          <div ref="msgBox" class="msg-list">
            <el-empty v-if="!msgs.length" :image-size="100" description="开始你的第一次对话吧" />
            <MessageItem
              v-for="(m, i) in msgs" :key="i"
              :msg="m" :index="i"
              @retry="retry(i)"
              @copy="copy(m)"
            />
            <div v-if="loading" class="typing">
              <span></span><span></span><span></span>
              <em>{{ thinkingText }}</em>
            </div>
          </div>

          <!-- 输入区 -->
          <div class="input-area">
            <el-input
              v-model="input"
              type="textarea"
              :rows="3"
              :placeholder="curId ? '说点什么…  (Enter 发送 / Shift+Enter 换行)' : '请先选择智能体'"
              :disabled="!curId || loading"
              @keydown.enter.exact.prevent="send"
            />
            <div class="input-actions">
              <el-button :disabled="loading" @click="stop">停止</el-button>
              <el-button type="primary" :loading="loading" :disabled="!input.trim()" @click="send">
                <el-icon><Promotion /></el-icon>发送
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ChatDotRound, Close, Delete, Connection, Promotion } from '@element-plus/icons-vue'
import http from '@/api/request'
import MessageItem from '@/components/MessageItem.vue'
import { useSettingsStore } from '@/store/settings'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

marked.setOptions({
  highlight: (c, lang) => {
    try { return hljs.highlight(c, { language: hljs.getLanguage(lang) ? lang : 'plaintext' }).value }
    catch { return c }
  },
  breaks: true,
  gfm: true
})

const agents = ref([])
const curId = ref(null)
const curAgent = computed(() => agents.value.find(a => a.id === curId.value))
const sessions = ref([])
const curSession = ref(null)
const msgs = ref([])
const input = ref('')
const loading = ref(false)
const msgBox = ref(null)
const multiMode = ref(false)
const settings = useSettingsStore()

const thinkingTexts = ['思考中…', '检索知识…', '调用工具…', '生成回答…', '多智能体协作中…']
const thinkingText = ref(thinkingTexts[0])
let thinkTimer = null
let abortCtl = null

const toolCount = (a) => {
  if (!a.toolIds) return ''
  const n = a.toolIds.split(',').length
  return `${n}个工具`
}

onMounted(async () => {
  try {
    const r = await http.get('/agent/list')
    agents.value = r.data || []
  } catch (e) { /* 拦截器已提示 */ }
  loadSessions()
})

const selectAgent = (a) => {
  if (curId.value === a.id) return
  curId.value = a.id
  msgs.value = []
  curSession.value = null
}
const newChat = () => { curId.value = null; curSession.value = null; msgs.value = []; input.value = '' }

const loadSessions = () => {
  const all = localStorage.getItem('chat_sessions')
  sessions.value = all ? JSON.parse(all) : []
  if (sessions.value.length > 50) sessions.value = sessions.value.slice(-50)
}
const saveSessions = () => localStorage.setItem('chat_sessions', JSON.stringify(sessions.value))

const loadSession = (s) => {
  curSession.value = s.id
  curId.value = s.agentId
  msgs.value = s.messages || []
  scroll()
}
const delSession = async (s) => {
  await ElMessageBox.confirm(`删除会话 "${s.title}"?`, '确认', { type: 'warning' })
  sessions.value = sessions.value.filter(x => x.id !== s.id)
  saveSessions()
  if (curSession.value === s.id) newChat()
}

const saveCurrent = () => {
  if (!msgs.value.length) return
  const id = curSession.value || ('s-' + Date.now())
  const title = msgs.value.find(m => m.role === 'user')?.content?.slice(0, 30) || '新对话'
  const idx = sessions.value.findIndex(x => x.id === id)
  const data = { id, agentId: curId.value, title, messages: msgs.value, updated: Date.now() }
  if (idx >= 0) sessions.value[idx] = data
  else sessions.value.push(data)
  curSession.value = id
  saveSessions()
  loadSessions()
}

const scroll = () => nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight })

const send = async () => {
  if (!input.value.trim() || !curId.value) return
  const q = input.value.trim()
  input.value = ''

  msgs.value.push({ role: 'user', content: q, time: Date.now() })
  const aiMsg = { role: 'assistant', content: '', actions: [], thought: '', time: Date.now(), streaming: true }
  msgs.value.push(aiMsg)
  loading.value = true
  scroll()
  startThinking()

  try {
    if (multiMode.value) {
      await runMultiAgent(q, aiMsg)
    } else if (settings.enableStream) {
      await runStream(q, aiMsg)
    } else {
      await runNormal(q, aiMsg)
    }
  } catch (e) {
    aiMsg.content = aiMsg.content || ('❌ ' + (e.message || '请求失败'))
    aiMsg.error = true
  } finally {
    aiMsg.streaming = false
    loading.value = false
    stopThinking()
    scroll()
    saveCurrent()
  }
}

const startThinking = () => {
  let i = 0
  thinkTimer = setInterval(() => {
    thinkingText.value = thinkingTexts[i++ % thinkingTexts.length]
  }, 2000)
}
const stopThinking = () => { clearInterval(thinkTimer); thinkTimer = null }

// 1. 普通同步调用
const runNormal = async (q, aiMsg) => {
  const r = await http.post('/agent/chat', null, { params: { agentId: curId.value, input: q } })
  aiMsg.content = r.data
}

// 2. 流式 (SSE via fetch)
const runStream = async (q, aiMsg) => {
  abortCtl = new AbortController()
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/agent/chat/stream?agentId=${curId.value}&input=${encodeURIComponent(q)}`, {
    headers: { 'Authorization': 'Bearer ' + token },
    signal: abortCtl.signal
  })
  if (!res.ok) throw new Error('HTTP ' + res.status)
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buf = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    const lines = buf.split('\n')
    buf = lines.pop() || ''
    for (const line of lines) {
      if (!line.startsWith('data:')) continue
      const data = line.slice(5).trim()
      if (data === '[DONE]') return
      try {
        const ev = JSON.parse(data)
        if (ev.type === 'content') aiMsg.content += ev.delta
        else if (ev.type === 'tool') aiMsg.actions.push(ev.action)
        else if (ev.type === 'thought') aiMsg.thought += ev.delta
        scroll()
      } catch (e) { /* ignore */ }
    }
  }
}

// 3. 多智能体 (Supervisor)
const runMultiAgent = async (q, aiMsg) => {
  aiMsg.content = '🧠 Supervisor 编排中...\n\n'
  const r = await http.post('/agent/multi/run', {
    mode: 'supervisor',
    sessionId: 'multi-' + Date.now(),
    input: q
  })
  const resp = r.data
  aiMsg.content = resp.content || ''
  aiMsg.thought = resp.thought || ''
  aiMsg.actions = resp.actions || []
  aiMsg.artifacts = resp.artifacts
  aiMsg.tokens = (resp.promptTokens || 0) + (resp.completionTokens || 0)
  aiMsg.elapsedMs = resp.elapsedMs
}

const stop = () => {
  if (abortCtl) { abortCtl.abort(); abortCtl = null }
  loading.value = false
  stopThinking()
  const last = msgs.value[msgs.value.length - 1]
  if (last && last.streaming) { last.streaming = false; last.content += '\n\n_(已停止)_' }
}

const retry = (i) => {
  const user = msgs.value[i - 1]
  if (!user || user.role !== 'user') return
  msgs.value = msgs.value.slice(0, i)
  input.value = user.content
  send()
}

const copy = async (m) => {
  await navigator.clipboard.writeText(m.content || '')
  ElMessage.success('已复制')
}

const clearCur = () => {
  msgs.value = []
  curSession.value = null
}

watch(multiMode, v => v && ElMessage.info('已开启多智能体模式 (Supervisor)'))
</script>

<style scoped>
.chat-container { padding: 12px; height: calc(100vh - 60px); }
.chat-row { height: 100%; }

.left-col, .right-col { height: 100%; }
.left-panel {
  background: #fff; padding: 12px; border-radius: 8px; height: 100%;
  display: flex; flex-direction: column; overflow: hidden;
}
.new-chat-btn { width: 100%; margin-bottom: 12px; }
.section-title { font-size: 12px; color: #999; margin: 8px 0 4px; padding: 0 4px; }
.agent-item, .session-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 10px;
  border-radius: 6px; cursor: pointer; font-size: 13px;
}
.agent-item:hover, .session-item:hover { background: var(--el-color-primary-light-9); }
.agent-item.active, .session-item.active { background: var(--el-color-primary-light-8); color: var(--el-color-primary); }
.agent-name, .session-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-item .del { opacity: 0; transition: opacity 0.2s; }
.session-item:hover .del { opacity: 1; }

.chat-panel { background: #fff; height: 100%; border-radius: 8px; display: flex; flex-direction: column; }
.chat-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-bottom: 1px solid var(--el-border-color-lighter);
}
.agent-info { display: flex; gap: 10px; align-items: center; }
.agent-info.empty { color: #999; }
.agent-title { font-weight: 600; font-size: 15px; }
.agent-sub { font-size: 12px; color: #999; }
.header-actions { display: flex; gap: 8px; align-items: center; }

.msg-list {
  flex: 1; overflow-y: auto; padding: 16px;
  background: var(--el-fill-color-blank);
}
.typing { display: flex; align-items: center; gap: 4px; padding: 8px 12px; color: #999; font-size: 12px; }
.typing span {
  display: inline-block; width: 6px; height: 6px; background: #aaa;
  border-radius: 50%; animation: bounce 1.2s infinite;
}
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%, 60%, 100% { transform: translateY(0); } 30% { transform: translateY(-6px); } }
.typing em { font-style: normal; margin-left: 8px; }

.input-area { padding: 12px 16px; border-top: 1px solid var(--el-border-color-lighter); }
.input-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
</style>
