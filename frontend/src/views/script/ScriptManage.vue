<template>
  <div class="script-manage">
    <div class="page-header">
      <h3>剧本管理</h3>
    </div>

    <el-row :gutter="16">
      <el-col :span="8">
        <div class="script-list" v-loading="loading">
          <div
            v-for="s in scripts"
            :key="s.id"
            :class="['script-item', { active: selected?.id === s.id }]"
            @click="selectScript(s)"
          >
            <span>{{ s.name || `剧本 #${s.id}` }}</span>
          </div>
          <el-empty v-if="!loading && scripts.length === 0" description="暂无剧本" />
        </div>
      </el-col>

      <el-col :span="16">
        <div v-if="selected" class="editor-area">
          <div class="editor-header">
            <span class="script-title">{{ selected.name }}</span>
            <div class="editor-actions">
              <el-button @click="handleGenerate" :loading="generating">AI生成</el-button>
              <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
            </div>
          </div>
          <el-input v-model="editContent" type="textarea" :rows="26" placeholder="剧本内容" />
          <div class="word-count">字数: {{ editContent.length }}</div>
        </div>
        <el-empty v-else description="选择一个剧本" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getScripts, getScript, updateScript, generateScript } from '@/api/script'
import { ElMessage } from 'element-plus'
import type { Script } from '@/types/novel'

const route = useRoute()
const projectId = Number(route.params.projectId)

const scripts = ref<Script[]>([])
const selected = ref<Script | null>(null)
const editContent = ref('')
const loading = ref(false)
const saving = ref(false)
const generating = ref(false)

async function load() {
  loading.value = true
  try {
    scripts.value = await getScripts(projectId)
  } finally {
    loading.value = false
  }
}

async function selectScript(s: Script) {
  const detail = await getScript(projectId, s.id)
  selected.value = detail
  editContent.value = detail.content || ''
}

async function handleSave() {
  if (!selected.value) return
  saving.value = true
  try {
    await updateScript(projectId, selected.value.id, { content: editContent.value })
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

async function handleGenerate() {
  if (!selected.value) return
  generating.value = true
  try {
    await generateScript(projectId, selected.value.id)
    ElMessage.success('生成完成')
    await selectScript(selected.value)
  } finally {
    generating.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.script-list { background: #fff; border-radius: 8px; padding: 12px; max-height: 700px; overflow-y: auto; }
.script-item { padding: 10px 12px; cursor: pointer; border-radius: 6px; }
.script-item:hover { background: #f5f7fa; }
.script-item.active { background: #ecf5ff; color: #409eff; }
.editor-area { background: #fff; border-radius: 8px; padding: 16px; }
.editor-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.script-title { font-size: 16px; font-weight: 600; }
.editor-actions { display: flex; gap: 8px; }
.word-count { text-align: right; color: #999; font-size: 12px; margin-top: 8px; }
</style>
