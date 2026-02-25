<template>
  <div class="storyboard-workbench">
    <!-- Agent 对话区 -->
    <div :class="['chat-section', { collapsed: chatCollapsed }]">
      <div class="section-header" @click="chatCollapsed = !chatCollapsed">
        <span>Agent 对话</span>
        <el-icon><ArrowUp v-if="!chatCollapsed" /><ArrowDown v-else /></el-icon>
      </div>
      <div v-show="!chatCollapsed" class="chat-body">
        <AgentChat
          :messages="chat.messages.value"
          :is-streaming="chat.isStreaming.value"
          :current-stream-text="chat.currentStreamText.value"
          @send="chat.sendMessage"
          @clean="chat.clearHistory"
        />
      </div>
    </div>

    <!-- 分镜预览区 -->
    <div class="storyboard-section">
      <div class="section-header">
        <span>分镜预览</span>
        <div class="section-actions">
          <el-button size="small" @click="handleSave">保存分镜</el-button>
          <el-button size="small" type="primary" @click="handleGenerateImages">生成图片</el-button>
        </div>
      </div>

      <div v-loading="loadingBoard" class="board-content">
        <div v-for="seg in segments" :key="seg.id" class="segment-block">
          <div class="segment-header">
            <span class="segment-title">片段{{ seg.segmentIndex }}: {{ seg.description }}</span>
            <el-tag size="small">{{ seg.emotion }}</el-tag>
          </div>
          <div class="shots-grid">
            <div v-for="shot in getShots(seg.id)" :key="shot.id" class="shot-card">
              <ImagePreview v-if="shot.imagePath" :src="shot.imagePath" width="140px" height="100px" />
              <div v-else class="shot-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
              <div class="shot-info">
                <span class="shot-index">镜头{{ shot.shotIndex }}</span>
                <span class="shot-motion">{{ shot.cameraMotion }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="segments.length === 0" description="通过 Agent 对话生成分镜" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAgentChat } from '@/composables/useAgentChat'
import { getStoryboards, saveStoryboards, generateImages } from '@/api/storyboard'
import AgentChat from '@/components/AgentChat.vue'
import ImagePreview from '@/components/ImagePreview.vue'
import { ElMessage } from 'element-plus'
import { ArrowUp, ArrowDown, Picture } from '@element-plus/icons-vue'
import type { Segment, Shot } from '@/types/novel'

const route = useRoute()
const projectId = route.params.projectId as string
const scriptId = route.params.scriptId as string

const chat = useAgentChat('storyboard', projectId, { scriptId })
chat.connect()

const segments = ref<Segment[]>([])
const shots = ref<Shot[]>([])
const loadingBoard = ref(false)
const chatCollapsed = ref(false)

function getShots(segmentId: number): Shot[] {
  return shots.value.filter(s => s.segmentId === segmentId).sort((a, b) => a.shotIndex - b.shotIndex)
}

async function loadBoard() {
  loadingBoard.value = true
  try {
    const res = await getStoryboards(Number(scriptId))
    segments.value = res?.segments || []
    shots.value = res?.shots || []
  } catch {
    segments.value = []
    shots.value = []
  } finally {
    loadingBoard.value = false
  }
}

async function handleSave() {
  await saveStoryboards(Number(scriptId), { segments: segments.value, shots: shots.value })
  ElMessage.success('保存成功')
}

async function handleGenerateImages() {
  const cells = shots.value.map(s => ({ shotId: s.id, prompt: s.prompt }))
  await generateImages(Number(scriptId), cells)
  ElMessage.success('图片生成任务已提交')
  setTimeout(loadBoard, 5000)
}

onMounted(loadBoard)
</script>

<style scoped>
.storyboard-workbench { display: flex; flex-direction: column; gap: 12px; }
.chat-section { background: #fff; border-radius: 8px; overflow: hidden; }
.chat-section.collapsed .chat-body { display: none; }
.chat-body { height: 280px; }
.section-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; cursor: pointer; border-bottom: 1px solid #e8e8e8;
  font-weight: 600;
}
.section-actions { display: flex; gap: 8px; }
.storyboard-section { background: #fff; border-radius: 8px; }
.board-content { padding: 16px; }
.segment-block { margin-bottom: 24px; }
.segment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.segment-title { font-weight: 600; }
.shots-grid { display: flex; gap: 12px; flex-wrap: wrap; }
.shot-card { width: 140px; }
.shot-placeholder {
  width: 140px; height: 100px; background: #f5f7fa; border-radius: 6px;
  display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 28px;
}
.shot-info { display: flex; justify-content: space-between; margin-top: 4px; font-size: 11px; color: #999; }
</style>
