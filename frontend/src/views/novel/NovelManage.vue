<template>
  <div class="novel-manage">
    <div class="page-header">
      <h3>小说管理</h3>
      <div class="header-actions">
        <el-button @click="showAdd = true">添加章节</el-button>
        <el-dropdown @command="handleExport">
          <el-button>导出</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="txt">导出 TXT</el-dropdown-item>
              <el-dropdown-item command="docx">导出 DOCX</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 章节列表 -->
      <el-col :span="8">
        <div class="chapter-list" v-loading="loading">
          <div
            v-for="n in novels"
            :key="n.id"
            :class="['chapter-item', { active: selected?.id === n.id }]"
            @click="selectChapter(n)"
          >
            <span class="chapter-index">第{{ n.chapterIndex }}章</span>
            <span class="chapter-name">{{ n.chapter }}</span>
          </div>
          <el-empty v-if="!loading && novels.length === 0" description="暂无章节" />
        </div>
      </el-col>

      <!-- 编辑区 -->
      <el-col :span="16">
        <div v-if="selected" class="editor-area">
          <div class="editor-header">
            <el-input v-model="editTitle" placeholder="章节标题" />
            <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          </div>
          <el-input
            v-model="editContent"
            type="textarea"
            :rows="24"
            placeholder="章节内容"
            class="editor-textarea"
          />
          <div class="word-count">字数: {{ editContent.length }}</div>
        </div>
        <el-empty v-else description="选择一个章节开始编辑" />
      </el-col>
    </el-row>

    <!-- 添加章节弹窗 -->
    <el-dialog v-model="showAdd" title="添加章节" width="500px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="章节序号">
          <el-input-number v-model="addForm.chapterIndex" :min="1" />
        </el-form-item>
        <el-form-item label="章节名">
          <el-input v-model="addForm.chapter" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="addForm.chapterData" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getNovels, getNovel, createNovel, updateNovel, exportNovel } from '@/api/novel'
import { ElMessage } from 'element-plus'
import type { Novel } from '@/types/novel'

const route = useRoute()
const projectId = Number(route.params.projectId)

const novels = ref<Novel[]>([])
const selected = ref<Novel | null>(null)
const editTitle = ref('')
const editContent = ref('')
const loading = ref(false)
const saving = ref(false)
const showAdd = ref(false)

const addForm = reactive({ chapterIndex: 1, chapter: '', chapterData: '' })

async function loadNovels() {
  loading.value = true
  try {
    novels.value = await getNovels(projectId)
  } finally {
    loading.value = false
  }
}

async function selectChapter(n: Novel) {
  const detail = await getNovel(projectId, n.id)
  selected.value = detail
  editTitle.value = detail.chapter
  editContent.value = detail.chapterData
}

async function handleSave() {
  if (!selected.value) return
  saving.value = true
  try {
    await updateNovel(projectId, selected.value.id, { chapter: editTitle.value, chapterData: editContent.value })
    ElMessage.success('保存成功')
    loadNovels()
  } finally {
    saving.value = false
  }
}

async function handleAdd() {
  await createNovel(projectId, addForm)
  ElMessage.success('添加成功')
  showAdd.value = false
  loadNovels()
}

async function handleExport(format: string) {
  if (format !== 'txt' && format !== 'docx') {
    ElMessage.warning('暂不支持该格式')
    return
  }
  try {
    const blob = await exportNovel(projectId, format)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `novel.${format}`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(loadNovels)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.header-actions { display: flex; gap: 8px; }
.chapter-list { background: #fff; border-radius: 8px; padding: 12px; max-height: 700px; overflow-y: auto; }
.chapter-item { padding: 10px 12px; cursor: pointer; border-radius: 6px; display: flex; gap: 8px; }
.chapter-item:hover { background: #f5f7fa; }
.chapter-item.active { background: #ecf5ff; color: #409eff; }
.chapter-index { color: #999; font-size: 13px; white-space: nowrap; }
.chapter-name { font-size: 14px; }
.editor-area { background: #fff; border-radius: 8px; padding: 16px; }
.editor-header { display: flex; gap: 12px; margin-bottom: 12px; }
.word-count { text-align: right; color: #999; font-size: 12px; margin-top: 8px; }
</style>
