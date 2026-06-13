<template>
  <el-card>
    <div style="display:flex;justify-content:space-between;margin-bottom:12px">
      <h3>工具 (Function Call)</h3>
    </div>
    <el-alert title="工具通过代码注册 + 数据库配置,使用 Spring Bean handler 反射调用" type="info" :closable="false"/>
    <el-table :data="builtIn" stripe style="margin-top:12px">
      <el-table-column prop="code" label="编码" width="160"/>
      <el-table-column prop="name" label="名称"/>
      <el-table-column prop="description" label="描述"/>
      <el-table-column prop="handler" label="Bean Handler" width="160"/>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
const builtIn = ref([
  { code: 'weather', name: '天气查询', description: '查询某城市天气', handler: 'weatherTool' }
])
</script>
