<template>
  <div class="video-generate">
    <div class="page-header">
      <h3>视频生成</h3>
      <div class="header-actions">
        <el-button @click="handleBatchRetry" :loading="retrying">批量重试失败</el-button>
        <el-button type="primary" @click="handleGenerate" :loading="generating">生成视频</el-button>
      </div>
    </div>

    <el-table :data="videos" v-loading="loading" stripe>
      <el-table-column prop="shotId" label="镜头ID" width="80" />
      <el-table-column prop="provider" label="模型" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="视频" width="200">
        <template #default="{ row }">
          <VideoPlayer v-if="row.videoUrl" :src="row.videoUrl" width="180px" max-height="120px" />
          <span v-else class="no-video">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="60" />
      <el-table-column label="选中" width="60">
        <template #default="{ row }">
          <el-tag v-if="row.selected" type="success" size="small">✓</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" @click="handleSelect(row)" :disabled="row.selected">选择</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getVideos, generateVideo, selectVideo, batchRetryVideos } from '@/api/video'
import VideoPlayer from '@/components/VideoPlayer.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const projectId = Number(route.params.projectId)
const scriptId = Number(route.params.scriptId)

const videos = ref<any[]>([])
const loading = ref(false)
const generating = ref(false)
const retrying = ref(false)

const statusMap: Record<string, { type: string; label: string }> = {
  pending: { type: 'info', label: '等待中' },
  processing: { type: 'warning', label: '生成中' },
  success: { type: 'success', label: '成功' },
  failed: { type: 'danger', label: '失败' },
}

async function load() {
  loading.value = true
  try {
    videos.value = await getVideos(projectId, scriptId)
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  generating.value = true
  try {
    await generateVideo(projectId, { scriptId })
    ElMessage.success('视频生成任务已提交')
    setTimeout(load, 5000)
  } finally {
    generating.value = false
  }
}

async function handleSelect(row: any) {
  await selectVideo(projectId, row.id)
  ElMessage.success('已选择')
  load()
}

async function handleBatchRetry() {
  retrying.value = true
  try {
    await batchRetryVideos(projectId)
    ElMessage.success('重试任务已提交')
    setTimeout(load, 3000)
  } finally {
    retrying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.header-actions { display: flex; gap: 8px; }
.no-video { color: #c0c4cc; }
</style>
