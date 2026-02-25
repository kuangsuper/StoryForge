<template>
  <div class="pipeline-view">
    <div class="page-header">
      <h3>全自动流水线</h3>
    </div>

    <!-- 参数配置 -->
    <el-card class="config-card">
      <el-form :model="params" label-width="100px" size="small" inline>
        <el-form-item label="题材">
          <el-select v-model="params.genre" placeholder="选择题材">
            <el-option v-for="g in genres" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="创作提示">
          <el-input v-model="params.prompt" placeholder="描述故事方向" style="width: 300px;" />
        </el-form-item>
        <el-form-item label="集数">
          <el-input-number v-model="params.episodeCount" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="视频模式">
          <el-select v-model="params.videoMode">
            <el-option label="单图" value="singleImage" />
            <el-option label="首尾帧" value="firstLastFrame" />
          </el-select>
        </el-form-item>
        <el-form-item label="自动合成">
          <el-switch v-model="params.autoCompose" />
        </el-form-item>
        <el-form-item label="AI配音">
          <el-switch v-model="params.ttsEnabled" />
        </el-form-item>
      </el-form>
      <div class="config-actions">
        <el-button type="primary" @click="handleStart" :loading="starting" :disabled="isRunning">
          启动全自动生产
        </el-button>
        <el-button v-if="isRunning" @click="handlePause">暂停</el-button>
        <el-button v-if="isRunning" type="danger" @click="handleCancel">取消</el-button>
      </div>
    </el-card>

    <!-- 流水线进度 -->
    <el-card class="progress-card" v-if="status">
      <template #header>流水线进度</template>
      <div class="step-list">
        <div v-for="(step, i) in status.steps" :key="i" class="step-item">
          <span class="step-icon">
            <span v-if="step.state === 'success'">✅</span>
            <span v-else-if="step.state === 'running'">🔄</span>
            <span v-else-if="step.state === 'failed'">❌</span>
            <span v-else-if="step.state === 'skipped'">⏭️</span>
            <span v-else>⏳</span>
          </span>
          <span class="step-name">Step {{ i + 1 }}: {{ step.displayName }}</span>
          <span v-if="step.duration" class="step-duration">{{ formatDuration(step.duration) }}</span>
          <span v-if="step.message" class="step-msg">{{ step.message }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { startPipeline, getPipelineStatus, terminatePipeline } from '@/api/pipeline'
import { usePipelineProgress } from '@/composables/usePipelineProgress'
import { formatDuration } from '@/utils/format'
import { ElMessage } from 'element-plus'
import type { PipelineStatus, ReviewMode } from '@/types/pipeline'

const route = useRoute()
const projectId = route.params.projectId as string

const genres = ['都市', '玄幻', '科幻', '言情', '悬疑', '无限流', '系统流']
const params = reactive({
  genre: '',
  prompt: '',
  episodeCount: 5,
  chaptersPerEpisode: 2,
  videoMode: 'singleImage',
  autoCompose: true,
  ttsEnabled: false,
  reviewConfig: {} as Record<string, ReviewMode>,
})

const status = ref<PipelineStatus | null>(null)
const isRunning = ref(false)
const starting = ref(false)

const progress = usePipelineProgress(projectId)

async function loadStatus() {
  try {
    status.value = await getPipelineStatus(Number(projectId))
    isRunning.value = status.value?.state === 'running'
  } catch {
    // no pipeline running
  }
}

async function handleStart() {
  starting.value = true
  try {
    await startPipeline(Number(projectId), params)
    ElMessage.success('流水线已启动')
    isRunning.value = true
    progress.connect()
    setTimeout(loadStatus, 2000)
  } finally {
    starting.value = false
  }
}

async function handlePause() {
  // 后端暂不支持暂停，使用终止代替
  await terminatePipeline(Number(projectId))
  ElMessage.success('已停止')
  loadStatus()
}

async function handleCancel() {
  await terminatePipeline(Number(projectId))
  ElMessage.success('已取消')
  isRunning.value = false
  loadStatus()
}

onMounted(loadStatus)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.config-card { margin-bottom: 16px; }
.config-actions { margin-top: 12px; display: flex; gap: 8px; }
.progress-card { margin-top: 16px; }
.step-list { display: flex; flex-direction: column; gap: 12px; }
.step-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.step-icon { font-size: 18px; }
.step-name { font-weight: 500; min-width: 200px; }
.step-duration { color: #999; font-size: 13px; }
.step-msg { color: #666; font-size: 13px; }
</style>
