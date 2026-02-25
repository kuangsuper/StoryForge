<template>
  <div class="storyline-view">
    <div class="page-header">
      <h3>故事线</h3>
      <div class="header-actions">
        <el-button type="primary" @click="handleSave" :loading="saving" :disabled="!content">保存</el-button>
        <el-button @click="handleDelete" :disabled="!content" type="danger">删除</el-button>
      </div>
    </div>
    <div class="editor-card" v-loading="loading">
      <el-input
        v-model="content"
        type="textarea"
        :rows="28"
        placeholder="故事线内容（可通过 Agent 对话自动生成）"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getStoryline, saveStoryline, deleteStoryline } from '@/api/storyline'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const projectId = Number(route.params.projectId)

const content = ref('')
const loading = ref(false)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getStoryline(projectId)
    content.value = res?.content || ''
  } catch {
    content.value = ''
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveStoryline(projectId, content.value)
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  await ElMessageBox.confirm('确定删除故事线？', '确认', { type: 'warning' })
  await deleteStoryline(projectId)
  content.value = ''
  ElMessage.success('已删除')
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.header-actions { display: flex; gap: 8px; }
.editor-card { background: #fff; border-radius: 8px; padding: 16px; }
</style>
