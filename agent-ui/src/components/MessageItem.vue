<template>
  <div :class="['msg', msg.role]">
    <el-avatar :size="36" :src="msg.role==='user' ? userAvatar : aiAvatar">
      {{ msg.role==='user' ? 'U' : 'AI' }}
    </el-avatar>
    <div class="msg-body">
      <!-- 用户消息 -->
      <div v-if="msg.role==='user'" class="bubble user-bubble" v-text="msg.content"></div>

      <!-- AI 消息 -->
      <div v-else class="bubble ai-bubble">
        <!-- 工具调用面板 -->
        <ToolCallPanel v-if="msg.actions && msg.actions.length" :actions="msg.actions" />

        <!-- 思考过程 (折叠) -->
        <details v-if="msg.thought" class="thought">
          <summary>🧠 思考过程</summary>
          <pre>{{ msg.thought }}</pre>
        </details>

        <!-- 主内容 (Markdown) -->
        <div class="md" v-html="render(msg.content)"></div>

        <!-- 元信息 (token / 耗时) -->
        <div v-if="msg.tokens || msg.elapsedMs" class="meta">
          <el-tag v-if="msg.tokens" size="small" type="info">{{ msg.tokens }} tokens</el-tag>
          <el-tag v-if="msg.elapsedMs" size="small" type="info">{{ msg.elapsedMs }}ms</el-tag>
        </div>

        <!-- 流式光标 -->
        <span v-if="msg.streaming" class="cursor">▊</span>
      </div>

      <!-- 操作按钮 (hover 显示) -->
      <div class="actions" v-if="msg.role==='assistant' && !msg.streaming">
        <el-tooltip content="复制">
          <el-button text size="small" @click="$emit('copy', msg)"><el-icon><CopyDocument /></el-icon></el-button>
        </el-tooltip>
        <el-tooltip content="重新生成">
          <el-button text size="small" @click="$emit('retry')"><el-icon><Refresh /></el-icon></el-button>
        </el-tooltip>
        <el-tooltip :content="msg.liked ? '已点赞' : '点赞'">
          <el-button text size="small" :type="msg.liked ? 'primary' : ''" @click="msg.liked = !msg.liked">
            <el-icon><Star v-if="msg.liked" /><StarFilled v-else /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip :content="msg.disliked ? '已踩' : '点踩'">
          <el-button text size="small" :type="msg.disliked ? 'danger' : ''" @click="msg.disliked = !msg.disliked">
            <el-icon><CircleClose v-if="msg.disliked" /><Warning v-else /></el-icon>
          </el-button>
        </el-tooltip>
      </div>

      <!-- 时间戳 -->
      <div class="time" v-if="msg.time">
        {{ formatTime(msg.time) }}
        <el-tag v-if="msg.error" size="small" type="danger">失败</el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'
import { CopyDocument, Refresh, Star, StarFilled, CircleClose, Warning } from '@element-plus/icons-vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import ToolCallPanel from './ToolCallPanel.vue'

const props = defineProps({ msg: { type: Object, required: true }, index: Number })
defineEmits(['copy', 'retry'])

const userAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=user&backgroundColor=b6e3f4'
const aiAvatar   = 'https://api.dicebear.com/7.x/bottts/svg?seed=ai&backgroundColor=ffd5dc'

marked.setOptions({
  highlight: (c, lang) => {
    try { return hljs.highlight(c, { language: hljs.getLanguage(lang) ? lang : 'plaintext' }).value }
    catch { return c }
  },
  breaks: true,
  gfm: true
})

const render = (c) => marked.parse(c || '')

const formatTime = (ts) => {
  const d = new Date(ts)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  return isToday
    ? d.toTimeString().slice(0, 5)
    : `${d.getMonth()+1}/${d.getDate()} ${d.toTimeString().slice(0,5)}`
}
</script>

<style scoped>
.msg { display: flex; gap: 10px; margin-bottom: 18px; }
.msg.user { flex-direction: row-reverse; }
.msg-body { max-width: 80%; }
.bubble { padding: 10px 14px; border-radius: 10px; word-wrap: break-word; }
.user-bubble { background: var(--el-color-primary); color: #fff; }
.ai-bubble {
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
}
.bubble.error { border-color: var(--el-color-danger); background: #fef0f0; }

.thought { margin: 0 0 8px; font-size: 12px; color: #888; }
.thought summary { cursor: pointer; padding: 4px 0; user-select: none; }
.thought pre { background: #f8f8f8; padding: 8px; border-radius: 4px; overflow-x: auto; max-height: 200px; font-size: 11px; }

.md { line-height: 1.7; }
.md :deep(h1) { font-size: 1.4em; margin: 0.5em 0; }
.md :deep(h2) { font-size: 1.2em; margin: 0.5em 0; }
.md :deep(h3) { font-size: 1.1em; margin: 0.5em 0; }
.md :deep(p)  { margin: 0.5em 0; }
.md :deep(ul), .md :deep(ol) { margin: 0.5em 0 0.5em 1.5em; }
.md :deep(code) { background: rgba(27,31,35,0.06); padding: 0.2em 0.4em; border-radius: 3px; font-size: 0.9em; }
.md :deep(pre) { background: #0d1117; color: #c9d1d9; padding: 12px; border-radius: 6px; overflow-x: auto; }
.md :deep(pre code) { background: transparent; color: inherit; padding: 0; }
.md :deep(blockquote) { border-left: 3px solid #ddd; padding-left: 12px; color: #666; }
.md :deep(table) { border-collapse: collapse; margin: 8px 0; }
.md :deep(th), .md :deep(td) { border: 1px solid #ddd; padding: 4px 8px; }
.md :deep(a) { color: var(--el-color-primary); }

.meta { margin-top: 8px; display: flex; gap: 4px; }
.cursor { animation: blink 1s infinite; color: var(--el-color-primary); }
@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }

.actions { display: flex; gap: 4px; opacity: 0; transition: opacity 0.2s; margin-top: 4px; }
.msg-body:hover .actions { opacity: 1; }
.time { font-size: 11px; color: #bbb; margin-top: 4px; }
</style>
