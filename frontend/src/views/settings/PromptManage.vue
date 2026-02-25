<template>
  <div class="prompt-manage">
    <div class="page-header">
      <h3>Prompt 管理</h3>
    </div>

    <el-table :data="prompts" v-loading="loading" stripe>
      <el-table-column prop="code" label="Code" width="250" />
      <el-table-column prop="name" label="名称" width="200" />
      <el-table-column label="自定义值" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.customValue || '(使用默认值)' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showEdit" title="编辑 Prompt" width="700px">
      <p class="edit-label">Code: {{ editRow?.code }}</p>
      <p class="edit-label">默认值:</p>
      <el-input :model-value="editRow?.defaultValue" type="textarea" :rows="6" disabled />
      <p class="edit-label" style="margin-top: 12px;">自定义值（留空使用默认值）:</p>
      <el-input v-model="editCustom" type="textarea" :rows="8" />
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPrompts, updatePrompt } from '@/api/prompt'
import { ElMessage } from 'element-plus'

const prompts = ref<any[]>([])
const loading = ref(false)
const showEdit = ref(false)
const editRow = ref<any>(null)
const editCustom = ref('')

async function load() {
  loading.value = true
  try { prompts.value = await getPrompts() } finally { loading.value = false }
}

function handleEdit(row: any) {
  editRow.value = row
  editCustom.value = row.customValue || ''
  showEdit.value = true
}

async function handleSave() {
  await updatePrompt(editRow.value.id, editCustom.value)
  ElMessage.success('保存成功')
  showEdit.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.edit-label { font-size: 13px; color: #666; margin: 4px 0; }
</style>
