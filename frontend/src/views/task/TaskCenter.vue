<template>
  <div class="task-center">
    <div class="page-header">
      <h3>任务中心</h3>
      <el-radio-group v-model="filterState" @change="load">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="pending">等待中</el-radio-button>
        <el-radio-button label="running">运行中</el-radio-button>
        <el-radio-button label="success">成功</el-radio-button>
        <el-radio-button label="failed">失败</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="tasks" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="type" label="类型" width="150" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="stateMap[row.state]" size="small">{{ row.state }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="120">
        <template #default="{ row }">
          <el-progress :percentage="row.progress || 0" :stroke-width="6" />
        </template>
      </el-table-column>
      <el-table-column prop="errorMsg" label="错误信息" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTasks } from '@/api/task'

const tasks = ref<any[]>([])
const loading = ref(false)
const filterState = ref('')

const stateMap: Record<string, string> = {
  pending: 'info',
  running: 'warning',
  success: 'success',
  failed: 'danger',
}

async function load() {
  loading.value = true
  try { tasks.value = await getTasks(filterState.value || undefined) } finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
