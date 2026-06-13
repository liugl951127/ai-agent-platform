import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue') },
  {
    path: '/', component: () => import('@/layout/Layout.vue'),
    redirect: '/chat',
    children: [
      { path: 'chat',      component: () => import('@/views/Chat.vue'),      meta: { title: '智能对话' } },
      { path: 'agents',    component: () => import('@/views/Agents.vue'),    meta: { title: '智能体' } },
      { path: 'models',    component: () => import('@/views/Models.vue'),    meta: { title: '模型管理' } },
      { path: 'knowledge', component: () => import('@/views/Knowledge.vue'), meta: { title: '知识库' } },
      { path: 'tools',     component: () => import('@/views/Tools.vue'),     meta: { title: '工具' } },
      { path: 'workflow',  component: () => import('@/views/Workflow.vue'),  meta: { title: '工作流' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to, _, next) => {
  const u = useUserStore()
  if (to.path === '/login') return next()
  if (!u.token) return next('/login')
  next()
})
export default router
