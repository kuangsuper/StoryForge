<template>
  <div class="outline-list">
    <div class="page-header">
      <h3>大纲管理</h3>
      <div class="header-actions">
        <el-button @click="handleExtractAssets" :loading="extracting">提取资产</el-button>
        <el-button type="primary" @click="showAdd = true">添加大纲</el-button>
      </div>
    </div>

    <div v-loading="loading" class="outline-grid">
      <el-card v-for="o in outlines" :key="o.id" class="outline-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>第{{ o.episode }}集 - {{ o.data?.title || '未命名' }}</span>
            <el-dropdown @command="(cmd: string) => handleAction(cmd, o)">
              <el-icon><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
        <p class="outline-text">{{ o.data?.outline || '暂无内容' }}</p>
        <div class="outline-meta">
          <el-tag v-if="o.data?.coreConflict" size="small" type="danger">{{ o.data.coreConflict }}</el-tag>
        </div>
      </el-card>
      <el-empty v-if="!loading && outlines.length === 0" description="暂无大纲，通过 Agent 对话生成" />
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="showEdit" :title="editForm.id ? '编辑大纲' : '添加大纲'" width="600px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="剧情主干">
          <el-input v-model="editForm.outline" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="核心矛盾">
          <el-input v-model="editForm.coreConflict" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOutlines, createOutline, updateOutline, deleteOutlines, extractAssets } from '@/api/outline'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'
import type { Outline } from '@/types/novel'

const route = useRoute()
const projectId = Number(route.params.projectId)

const outlines = ref<Outline[]>([])
const loading = ref(false)
const extracting = ref(false)
const showAdd = ref(false)
const showEdit = ref(false)
const editForm = reactive({ id: 0, title: '', outline: '', coreConflict: '' })

async function load() {
  loading.value = true
  try {
    outlines.value = await getOutlines(projectId)
  } finally {
    loading.value = false
  }
}

function handleAction(cmd: string, o: Outline) {
  if (cmd === 'edit') {
    editForm.id = o.id
    editForm.title = o.data?.title || ''
    editForm.outline = o.data?.outline || ''
    editForm.coreConflict = o.data?.coreConflict || ''
    showEdit.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm('确定删除？', '确认', { type: 'warning' }).then(async () => {
      await deleteOutlines(projectId, [o.id])
      ElMessage.success('已删除')
      load()
    })
  }
}

async function handleSaveEdit() {
  if (editForm.id) {
    await updateOutline(projectId, editForm.id, { title: editForm.title, outline: editForm.outline, coreConflict: editForm.coreConflict })
  } else {
    await createOutline(projectId, { title: editForm.title, outline: editForm.outline, coreConflict: editForm.coreConflict })
  }
  ElMessage.success('保存成功')
  showEdit.value = false
  load()
}

async function handleExtractAssets() {
  extracting.value = true
  try {
    await extractAssets(projectId)
    ElMessage.success('资产提取完成')
  } finally {
    extracting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.header-actions { display: flex; gap: 8px; }
.outline-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.outline-card { cursor: default; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.outline-text { color: #666; font-size: 13px; line-height: 1.6; margin: 0 0 8px; }
.outline-meta { display: flex; gap: 6px; flex-wrap: wrap; }
</style>
