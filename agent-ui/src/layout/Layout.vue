<template>
  <el-container style="height:100vh">
    <el-aside width="220px" class="aside">
      <div class="logo">🤖 Agent 平台</div>
      <el-menu :default-active="route.path" router background-color="#001529" text-color="#fff">
        <el-menu-item v-for="m in menus" :key="m.path" :index="'/'+m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ route.meta.title }}</span>
        <el-dropdown @command="cmd">
          <span class="user">👤 admin</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main><router-view/></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ChatDotRound, Avatar, SetUp, Files, Tools, Connection } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const menus = [
  { path: 'chat',      title: '智能对话', icon: ChatDotRound },
  { path: 'agents',    title: '智能体',   icon: Avatar },
  { path: 'models',    title: '模型',     icon: SetUp },
  { path: 'knowledge', title: '知识库',   icon: Files },
  { path: 'tools',     title: '工具',     icon: Tools },
  { path: 'workflow',  title: '工作流',   icon: Connection }
]
const cmd = (c) => { if (c === 'logout') { user.logout(); router.push('/login') } }
</script>

<style scoped>
.aside { background: #001529; color: #fff; }
.logo  { height: 60px; line-height: 60px; text-align: center; font-size: 18px; color: #fff; }
.header { display: flex; justify-content: space-between; align-items: center;
          background: #fff; border-bottom: 1px solid #eee; }
.user   { cursor: pointer; }
.el-main{ background: #f5f7fa; }
</style>
