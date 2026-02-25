<template>
  <div class="model-config">
    <div class="page-header">
      <h3>AI 模型配置</h3>
      <el-button type="primary" @click="showAdd = true">添加模型</el-button>
    </div>

    <el-table :data="models" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称" width="150" />
      <el-table-column prop="provider" label="提供商" width="120" />
      <el-table-column prop="modelName" label="模型" width="180" />
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="baseUrl" label="Base URL" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="handleTest(row)">测试</el-button>
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showAdd" :title="editForm.id ? '编辑模型' : '添加模型'" width="550px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="提供商"><el-input v-model="editForm.provider" /></el-form-item>
        <el-form-item label="模型名"><el-input v-model="editForm.modelName" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editForm.type">
            <el-option label="文本" value="text" />
            <el-option label="图片" value="image" />
            <el-option label="视频" value="video" />
            <el-option label="TTS" value="tts" />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key"><el-input v-model="editForm.apiKey" type="password" show-password /></el-form-item>
        <el-form-item label="Base URL"><el-input v-model="editForm.baseUrl" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getModelConfigs, createModelConfig, updateModelConfig, deleteModelConfig, testModelConfig } from '@/api/config'
import { ElMessage, ElMessageBox } from 'element-plus'

const models = ref<any[]>([])
const loading = ref(false)
const showAdd = ref(false)
const editForm = reactive({ id: 0, name: '', provider: '', modelName: '', type: 'text', apiKey: '', baseUrl: '' })

async function load() {
  loading.value = true
  try { models.value = await getModelConfigs() } finally { loading.value = false }
}

function handleEdit(row: any) {
  Object.assign(editForm, row)
  showAdd.value = true
}

async function handleSave() {
  if (editForm.id) {
    await updateModelConfig(editForm.id, editForm)
  } else {
    await createModelConfig(editForm)
  }
  ElMessage.success('保存成功')
  showAdd.value = false
  editForm.id = 0
  load()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除？', '确认', { type: 'warning' })
  await deleteModelConfig(row.id)
  ElMessage.success('已删除')
  load()
}

async function handleTest(row: any) {
  try {
    await testModelConfig(row.id)
    ElMessage.success('连接测试成功')
  } catch {
    ElMessage.error('连接测试失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
