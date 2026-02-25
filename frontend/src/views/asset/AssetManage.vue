<template>
  <div class="asset-manage">
    <div class="page-header">
      <h3>资产管理</h3>
      <div class="header-actions">
        <el-radio-group v-model="filterType" @change="load">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="role">角色</el-radio-button>
          <el-radio-button label="props">道具</el-radio-button>
          <el-radio-button label="scene">场景</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="showAdd = true">添加资产</el-button>
      </div>
    </div>

    <el-table :data="assets" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称" width="150" />
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="typeTagMap[row.type]" size="small">{{ typeNameMap[row.type] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="intro" label="描述" show-overflow-tooltip />
      <el-table-column label="图片" width="100">
        <template #default="{ row }">
          <ImagePreview v-if="row.filePath" :src="row.filePath" width="60px" height="60px" />
          <span v-else class="no-image">无</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="handleGenImage(row)">生成图片</el-button>
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="showAdd" :title="editForm.id ? '编辑资产' : '添加资产'" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="editForm.type">
            <el-option label="角色" value="role" />
            <el-option label="道具" value="props" />
            <el-option label="场景" value="scene" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="editForm.intro" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="提示词"><el-input v-model="editForm.prompt" type="textarea" :rows="3" /></el-form-item>
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
import { useRoute } from 'vue-router'
import { getAssets, createAsset, updateAsset, deleteAsset, generateAssetImage } from '@/api/asset'
import { ElMessage, ElMessageBox } from 'element-plus'
import ImagePreview from '@/components/ImagePreview.vue'
import type { Asset } from '@/types/novel'

const route = useRoute()
const projectId = Number(route.params.projectId)

const assets = ref<Asset[]>([])
const loading = ref(false)
const showAdd = ref(false)
const filterType = ref('')
const editForm = reactive({ id: 0, name: '', type: 'role' as 'role' | 'props' | 'scene', intro: '', prompt: '' })

const typeTagMap: Record<string, string> = { role: '', props: 'warning', scene: 'success' }
const typeNameMap: Record<string, string> = { role: '角色', props: '道具', scene: '场景' }

async function load() {
  loading.value = true
  try {
    assets.value = await getAssets(projectId, filterType.value || undefined)
  } finally {
    loading.value = false
  }
}

function handleEdit(row: Asset) {
  editForm.id = row.id
  editForm.name = row.name
  editForm.type = row.type
  editForm.intro = row.intro
  editForm.prompt = row.prompt
  showAdd.value = true
}

async function handleSave() {
  if (editForm.id) {
    await updateAsset(projectId, editForm.id, editForm)
  } else {
    await createAsset(projectId, editForm)
  }
  ElMessage.success('保存成功')
  showAdd.value = false
  editForm.id = 0
  load()
}

async function handleDelete(row: Asset) {
  await ElMessageBox.confirm(`确定删除「${row.name}」？`, '确认', { type: 'warning' })
  await deleteAsset(projectId, row.id)
  ElMessage.success('已删除')
  load()
}

async function handleGenImage(row: Asset) {
  try {
    await generateAssetImage(projectId, row.id)
    ElMessage.success('图片生成任务已提交')
    setTimeout(load, 3000)
  } catch {
    ElMessage.error('生成失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.page-header h3 { margin: 0; }
.header-actions { display: flex; gap: 12px; align-items: center; }
.no-image { color: #c0c4cc; font-size: 12px; }
</style>
