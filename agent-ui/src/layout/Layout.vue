<template>
  <el-container class="layout" :class="{ dark: isDark, collapsed: asideCollapsed }">
    <!-- 侧边栏 -->
    <el-aside :width="asideCollapsed ? '64px' : '220px'" class="aside">
      <div class="logo" @click="asideCollapsed = !asideCollapsed">
        <span class="logo-icon">🤖</span>
        <span v-show="!asideCollapsed" class="logo-text">Agent 平台</span>
      </div>
      <el-menu :default-active="route.path" router :collapse="asideCollapsed" background-color="transparent" text-color="#fff" active-text-color="#409EFF">
        <el-menu-item v-for="m in menus" :key="m.path" :index="'/'+m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <template #title>{{ m.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 头部 -->
      <el-header class="header">
        <div class="left">
          <el-icon class="trigger" @click="asideCollapsed = !asideCollapsed">
            <Expand v-if="asideCollapsed" /><Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '页面' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="right">
          <el-tooltip :content="isDark ? '切换亮色' : '切换暗色'">
            <el-button text @click="toggleDark">
              <el-icon><Moon v-if="!isDark" /><Sunny v-else /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="全屏">
            <el-button text @click="toggleFullscreen">
              <el-icon><FullScreen v-if="!isFullscreen" /><Aim v-else /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="服务状态">
            <el-button text @click="showStatus = true">
              <el-icon><CircleCheck v-if="healthOk" /><Warning v-else /></el-icon>
            </el-button>
          </el-tooltip>
          <el-dropdown @command="cmd" trigger="click">
            <span class="user">
              <el-avatar :size="32" :src="userAvatar">A</el-avatar>
              <span class="user-name">{{ user.user?.name || 'admin' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- 服务状态抽屉 -->
    <el-drawer v-model="showStatus" title="服务状态" direction="rtl" size="400px">
      <el-table :data="services" stripe>
        <el-table-column prop="name" label="服务" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 'UP' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="port" label="端口" width="70" />
      </el-table>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useSettingsStore } from '@/store/settings'
import { ChatDotRound, Avatar, SetUp, Files, Tools, Connection, Expand, Fold, Moon, Sunny, FullScreen, Aim, CircleCheck, Warning, ArrowDown } from '@element-plus/icons-vue'
import http from '@/api/request'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const settings = useSettingsStore()

const asideCollapsed = ref(false)
const isFullscreen = ref(false)
const showStatus = ref(false)
const services = ref([])
const healthOk = computed(() => services.value.every(s => s.status === 'UP'))

const userAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin'

const menus = [
  { path: 'chat',      title: '智能对话', icon: ChatDotRound },
  { path: 'agents',    title: '智能体',   icon: Avatar },
  { path: 'models',    title: '模型',     icon: SetUp },
  { path: 'knowledge', title: '知识库',   icon: Files },
  { path: 'tools',     title: '工具',     icon: Tools },
  { path: 'workflow',  title: '工作流',   icon: Connection }
]

const isDark = computed({
  get: () => settings.darkMode,
  set: v => settings.setDark(v)
})

const toggleDark = () => { settings.setDark(!isDark.value); applyTheme() }
const applyTheme = () => { document.documentElement.classList.toggle('dark', isDark.value) }

const toggleFullscreen = () => {
  if (!document.fullscreenElement) { document.documentElement.requestFullscreen(); isFullscreen.value = true }
  else { document.exitFullscreen(); isFullscreen.value = false }
}

const cmd = async (c) => {
  if (c === 'logout') {
    await ElMessageBox.confirm('确认退出登录?', '提示', { type: 'warning' })
    user.logout()
  } else if (c === 'profile') {
    ElMessage.info('个人中心 (待开发)')
  } else if (c === 'settings') {
    router.push('/settings')
  }
}

const checkHealth = async () => {
  // 探测所有服务 health 端点
  const svcs = [
    { name: 'gateway',    port: 9000 },
    { name: 'auth',       port: 9001 },
    { name: 'llm',        port: 9002 },
    { name: 'workflow',   port: 9003 },
    { name: 'knowledge',  port: 9004 },
    { name: 'agent',      port: 9005 },
    { name: 'conversation', port: 9006 },
    { name: 'system',     port: 9007 },
    { name: 'tools',      port: 9008 }
  ]
  // 用相对路径 (前端代理到对应服务, 这里假设 nginx 已配)
  // 实际场景应从 settings.serviceList 读
  const list = []
  for (const s of svcs) {
    try {
      const r = await fetch(`http://localhost:${s.port}/actuator/health`, { mode: 'cors' }).catch(() => null)
      if (r && r.ok) {
        const j = await r.json()
        list.push({ ...s, status: j.status || 'UP' })
      } else {
        list.push({ ...s, status: 'DOWN' })
      }
    } catch (e) { list.push({ ...s, status: 'DOWN' }) }
  }
  services.value = list
}

onMounted(() => {
  applyTheme()
  checkHealth()
  setInterval(checkHealth, 30_000)
})

watch(() => route.path, () => {
  // 移动端点导航后自动收起
  if (window.innerWidth < 768) asideCollapsed.value = true
})
</script>

<style>
/* 全局暗色变量 */
:root {
  --layout-bg: #f5f7fa;
  --header-bg: #fff;
  --aside-bg: #001529;
  --aside-text: #fff;
  --main-bg: #f5f7fa;
}
:root.dark {
  --layout-bg: #1a1a1a;
  --header-bg: #1f1f1f;
  --aside-bg: #0d0d0d;
  --aside-text: #d0d0d0;
  --main-bg: #1a1a1a;
  --el-bg-color: #2a2a2a;
  --el-bg-color-page: #1a1a1a;
  --el-text-color-primary: #e0e0e0;
  --el-border-color-lighter: #333;
  --el-fill-color-blank: #2a2a2a;
  --el-fill-color-light: #333;
  color-scheme: dark;
}

body { margin: 0; }
</style>

<style scoped>
.layout { height: 100vh; }
.aside { background: var(--aside-bg); color: var(--aside-text); transition: width 0.2s; overflow-x: hidden; }
.aside :deep(.el-menu) { border-right: 0; }

.logo {
  height: 60px; display: flex; align-items: center; justify-content: center; gap: 8px;
  color: #fff; cursor: pointer; user-select: none; border-bottom: 1px solid rgba(255,255,255,0.1);
}
.logo-icon { font-size: 24px; }
.logo-text { font-size: 16px; font-weight: 600; white-space: nowrap; }

.header {
  display: flex; justify-content: space-between; align-items: center;
  background: var(--header-bg); border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 0 16px;
}
.header .left { display: flex; align-items: center; gap: 12px; }
.trigger { cursor: pointer; font-size: 18px; }
.header .right { display: flex; align-items: center; gap: 8px; }
.user { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.user-name { font-size: 14px; }

.main { background: var(--main-bg); padding: 16px; overflow: auto; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 移动端 */
@media (max-width: 768px) {
  .aside { position: fixed; z-index: 100; height: 100vh; }
  .aside:not(.open) { transform: translateX(-100%); width: 0 !important; }
  .header { padding: 0 8px; }
  .user-name { display: none; }
}
</style>
