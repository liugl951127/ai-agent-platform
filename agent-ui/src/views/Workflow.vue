<template>
  <div>
    <el-card>
      <h2>🔄 工作流 (Flowable)</h2>
      <p class="sub">基于 Flowable 7 的 BPMN 流程引擎. 当前默认流程: agentChat (RAG → LLM → 工具调用 → 结束).</p>

      <el-row :gutter="12" style="margin-top:12px">
        <el-col :span="14">
          <h3>已部署流程</h3>
          <el-table :data="processes" stripe v-loading="loading">
            <el-table-column prop="id" label="ID" />
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="key" label="Key" />
            <el-table-column prop="version" label="版本" width="70" />
            <el-table-column prop="deploymentTime" label="部署时间" width="170" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button text type="primary" @click="view(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-col>

        <el-col :span="10">
          <h3>流程图 (agentChat)</h3>
          <el-card class="diagram" shadow="never">
            <el-steps direction="vertical" :active="5" finish-status="success">
              <el-step title="开始" description="用户输入" />
              <el-step title="RAG 检索" description="从知识库拉取上下文" />
              <el-step title="LLM 推理" description="ReAct + 工具调用" />
              <el-step title="工具调用" description="若 LLM 决定调工具" />
              <el-step title="结束" description="返回最终回答" />
            </el-steps>
          </el-card>
        </el-col>
      </el-row>

      <h3 style="margin-top:24px">运行实例</h3>
      <el-table :data="instances" stripe v-loading="loadingInst">
        <el-table-column prop="id" label="实例ID" />
        <el-table-column prop="processDefinitionKey" label="流程Key" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="结束时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.endTime ? 'success' : 'warning'">
              {{ row.endTime ? '已完成' : '运行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button text @click="viewInstance(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/request'

const processes = ref([])
const instances = ref([])
const loading = ref(false)
const loadingInst = ref(false)

const load = async () => {
  loading.value = true
  try { const r = await http.get('/workflow/process/list'); processes.value = r.data || [] } catch (e) {} finally { loading.value = false }
  loadingInst.value = true
  try { const r = await http.get('/workflow/instance/list'); instances.value = r.data || [] } catch (e) {} finally { loadingInst.value = false }
}

const view = (p) => { ElMessage.info('查看流程图: ' + p.name) }
const viewInstance = (p) => { ElMessage.info('查看实例: ' + p.id) }

onMounted(load)
</script>

<style scoped>
h2 { margin: 0 0 4px; }
h3 { margin: 16px 0 8px; font-size: 14px; }
.sub { color: #666; font-size: 13px; margin: 0 0 16px; }
.diagram { background: #fafafa; }
</style>
