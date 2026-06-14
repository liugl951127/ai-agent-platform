<template>
  <div class="tool-panel">
    <div class="panel-header" @click="expanded = !expanded">
      <el-icon class="toggle"><ArrowDown v-if="expanded" /><ArrowRight v-else /></el-icon>
      <span class="title">🔧 调用了 {{ actions.length }} 个工具</span>
      <el-tag size="small" :type="successCount === actions.length ? 'success' : 'warning'">
        {{ successCount }}/{{ actions.length }} 成功
      </el-tag>
    </div>
    <div v-show="expanded" class="panel-body">
      <div v-for="(a, i) in actions" :key="i" :class="['tool-item', a.ok ? 'ok' : 'err']">
        <el-icon class="tool-icon">
          <component :is="a.type === 'delegate' ? 'Connection' : 'Tools'" />
        </el-icon>
        <div class="tool-info">
          <div class="tool-name">
            {{ a.type === 'delegate' ? '派发给' : '工具' }} <b>{{ a.name }}</b>
            <el-tag v-if="a.elapsedMs" size="small" type="info">{{ a.elapsedMs }}ms</el-tag>
            <el-tag v-if="a.ok" size="small" type="success">成功</el-tag>
            <el-tag v-else size="small" type="danger">失败</el-tag>
          </div>
          <details v-if="a.input">
            <summary>入参</summary>
            <pre>{{ format(a.input) }}</pre>
          </details>
          <details v-if="a.output !== undefined">
            <summary>出参</summary>
            <pre>{{ format(a.output) }}</pre>
          </details>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ArrowDown, ArrowRight, Connection, Tools } from '@element-plus/icons-vue'

const props = defineProps({ actions: { type: Array, default: () => [] } })
const expanded = ref(true)
const successCount = computed(() => props.actions.filter(a => a.ok).length)

const format = (v) => {
  if (v === null || v === undefined) return ''
  if (typeof v === 'string') return v
  try { return JSON.stringify(v, null, 2) } catch { return String(v) }
}
</script>

<style scoped>
.tool-panel { margin-bottom: 10px; background: #f8f9fa; border: 1px solid #e9ecef; border-radius: 6px; overflow: hidden; }
.panel-header { display: flex; align-items: center; gap: 8px; padding: 6px 10px; cursor: pointer; user-select: none; }
.panel-header:hover { background: #eef2f7; }
.toggle { color: #666; }
.title { flex: 1; font-size: 13px; font-weight: 500; }
.panel-body { padding: 4px 10px 10px; }

.tool-item { display: flex; gap: 8px; padding: 6px 0; border-bottom: 1px dashed #eee; }
.tool-item:last-child { border-bottom: none; }
.tool-icon { color: #1976d2; flex-shrink: 0; margin-top: 2px; }
.tool-item.err .tool-icon { color: #d32f2f; }
.tool-info { flex: 1; min-width: 0; }
.tool-name { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; font-size: 13px; }

details { font-size: 12px; color: #666; margin-top: 4px; }
details summary { cursor: pointer; user-select: none; padding: 2px 0; }
details[open] summary { color: #1976d2; }
pre { background: #fff; padding: 6px 8px; border-radius: 4px; overflow-x: auto; max-height: 200px; margin: 4px 0 0; font-size: 11px; border: 1px solid #e0e0e0; }
</style>
