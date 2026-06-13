<template>
  <div class="chat">
    <el-row :gutter="12" style="height: calc(100vh - 120px)">
      <el-col :span="5" class="left">
        <el-button type="primary" size="small" @click="newChat" style="width:100%">+ 新对话</el-button>
        <div v-for="a in agents" :key="a.id"
             :class="['agent', curId===a.id && 'active']" @click="sel(a)">
          <el-avatar :size="32" :src="a.avatar">{{ a.name?.[0] }}</el-avatar>
          <span>{{ a.name }}</span>
        </div>
      </el-col>
      <el-col :span="19" class="right">
        <div ref="box" class="msgs">
          <div v-for="(m,i) in msgs" :key="i" :class="['msg', m.role]">
            <el-avatar :size="32" :src="m.role==='user' ? userAvatar : aiAvatar"></el-avatar>
            <div class="bubble" v-html="render(m.content)"></div>
          </div>
        </div>
        <div class="input">
          <el-input v-model="input" type="textarea" :rows="3"
                    placeholder="说点什么…"
                    @keydown.enter.exact.prevent="send"/>
          <el-button type="primary" :loading="loading" @click="send">发送</el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref, nextTick } from 'vue'
import http from '@/api/request'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

marked.setOptions({ highlight: (c) => hljs.highlightAuto(c).value })

const agents = ref([]); const curId = ref(null)
const msgs   = ref([]); const input  = ref('')
const loading = ref(false); const box = ref()
const userAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=user'
const aiAvatar   = 'https://api.dicebear.com/7.x/bottts/svg?seed=ai'

const render = (c) => marked.parse(c || '')

onMounted(async () => { agents.value = (await http.get('/agent/list')).data })

const sel = (a) => { curId.value = a.id; msgs.value = [] }
const newChat = () => { curId.value = null; msgs.value = []; input.value = '' }

const send = async () => {
  if (!input.value.trim() || !curId.value) return
  const q = input.value; input.value = ''
  msgs.value.push({ role: 'user', content: q })
  msgs.value.push({ role: 'assistant', content: '' })
  loading.value = true
  await nextTick(); scroll()
  try {
    const r = await http.post('/agent/chat', null, { params: { agentId: curId.value, input: q } })
    msgs.value[msgs.value.length - 1].content = r.data
  } finally {
    loading.value = false; scroll()
  }
}
const scroll = () => nextTick(() => { box.value.scrollTop = box.value.scrollHeight })
</script>

<style scoped>
.chat { padding: 12px; }
.left { background:#fff; padding:12px; border-radius:8px; overflow-y:auto; }
.agent { display:flex; gap:8px; align-items:center; padding:8px; cursor:pointer; border-radius:6px; }
.agent.active, .agent:hover { background:#ecf5ff; }
.right { display:flex; flex-direction:column; background:#fff; border-radius:8px; }
.msgs  { flex:1; overflow-y:auto; padding:16px; background:#fafafa; }
.msg   { display:flex; gap:8px; margin-bottom:14px; }
.msg.user { flex-direction: row-reverse; }
.bubble { max-width:75%; padding:10px 14px; border-radius:8px; background:#fff; box-shadow:0 1px 2px #eee; }
.msg.user .bubble { background:#409eff; color:#fff; }
.input { display:flex; gap:8px; padding:10px; border-top:1px solid #eee; }
</style>
