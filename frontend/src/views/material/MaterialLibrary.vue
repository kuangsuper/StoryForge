<template>
  <div class="material-library">
    <div class="page-header">
      <h3>素材库</h3>
      <div class="header-actions">
        <el-radio-group v-model="filterType" @change="load">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="bgm">BGM</el-radio-button>
          <el-radio-button label="sfx">音效</el-radio-button>
          <el-radio-button label="intro">片头</el-radio-button>
          <el-radio-button label="outro">片尾</el-radio-button>
          <el-radio-button label="watermark">水印</el-radio-button>
        </el-radio-group>
        <el-upload :show-file-list="false" :before-upload="handleUpload">
          <el-button type="primary">上传素材</el-button>
        </el-upload>
      </div>
    </div>

    <el-table :data="materials" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件" width="200">
        <template #default="{ row }">
          <a :href="row.filePath" target="_blank" class="file-link">{{ row.filePath?.split('/').pop() }}</a>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" width="180" />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMaterials, uploadMaterial, deleteMaterial } from '@/api/material'
import { ElMessage, ElMessageBox } from 'element-plus'

const materials = ref<any[]>([])
const loading = ref(false)
const filterType = ref('')

async function load() {
  loading.value = true
  try { materials.value = await getMaterials(filterType.value || undefined) } finally { loading.value = false }
}

function handleUpload(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('type', filterType.value || 'bgm')
  uploadMaterial(fd).then(() => {
    ElMessage.success('上传成功')
    load()
  })
  return false
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除？', '确认', { type: 'warning' })
  await deleteMaterial(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.page-header h3 { margin: 0; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.file-link { color: #409eff; text-decoration: none; }
</style>
