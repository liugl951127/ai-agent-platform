<template>
  <el-card>
    <h3>知识库</h3>
    <el-input v-model="kbId" placeholder="知识库 ID" style="width:160px"/>
    <el-input v-model="content" type="textarea" :rows="4" placeholder="文档片段内容"/>
    <el-button type="primary" @click="index" style="margin-top:8px">写入</el-button>
    <el-divider/>
    <el-input v-model="q" placeholder="检索问题" style="width:300px"/>
    <el-button @click="search">检索</el-button>
    <pre>{{ result }}</pre>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import http from '@/api/request'
import { ElMessage } from 'element-plus'

const kbId = ref(1), content = ref(''), q = ref(''), result = ref('')
const index  = async () => { await http.post('/knowledge/index', null, { params: { kbId: kbId.value, content: content.value }}); ElMessage.success('OK') }
const search = async () => { result.value = JSON.stringify((await http.post('/knowledge/search', null, { params: { kbId: kbId.value, q: q.value } })).data, null, 2) }
</script>
