<template>
  <el-card>
    <div style="display:flex;justify-content:space-between;margin-bottom:12px">
      <h3>智能体</h3>
      <el-button type="primary" @click="add">+ 新建</el-button>
    </div>
    <el-table :data="list" stripe>
      <el-table-column prop="name" label="名称"/>
      <el-table-column prop="modelId" label="模型ID" width="100"/>
      <el-table-column prop="toolIds" label="工具ID"/>
      <el-table-column prop="knowledgeId" label="知识库ID" width="120"/>
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button size="small" @click="test(row)">测试</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dlg" title="新建智能体" width="540">
      <el-form :model="form" label-width="90">
        <el-form-item label="名称"><el-input v-model="form.name"/></el-form-item>
        <el-form-item label="系统提示词"><el-input v-model="form.systemPrompt" type="textarea" :rows="3"/></el-form-item>
        <el-form-item label="模型">
          <el-select v-model="form.modelId">
            <el-option v-for="m in models" :key="m.id" :label="m.name" :value="m.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="知识库ID"><el-input-number v-model="form.knowledgeId" :min="0"/></el-form-item>
        <el-form-item label="工具IDs"><el-input v-model="form.toolIds" placeholder="逗号分隔"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([]), models = ref([])
const dlg = ref(false), form = reactive({ name:'', systemPrompt:'', modelId:null, toolIds:'', knowledgeId:null })
const reload = async () => list.value = (await http.get('/agent/list')).data
const loadModels = async () => models.value = (await http.get('/llm/list')).data
onMounted(async () => { await reload(); await loadModels() })

const add = () => { Object.assign(form, { name:'', systemPrompt:'', modelId:null, toolIds:'', knowledgeId:null }); dlg.value = true }
const save = async () => { await http.post('/agent/save', form); ElMessage.success('已保存'); dlg.value = false; reload() }
const del = async (row) => { await ElMessageBox.confirm('确认删除?','提示')
  ; await http.post('/agent/save', { ...row, deleted: 1 }); reload() }
const test = async (row) => {
  const r = await http.post('/agent/chat', null, { params: { agentId: row.id, input: '你好' } })
  ElMessageBox.alert(r.data, '回复')
}
</script>
