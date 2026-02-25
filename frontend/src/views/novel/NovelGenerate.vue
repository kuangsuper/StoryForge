<template>
  <div class="novel-generate">
    <el-row :gutter="16">
      <!-- 左侧：参数 + Agent 对话 -->
      <el-col :span="10">
        <div class="params-panel">
          <h4>创作参数</h4>
          <el-form :model="params" label-width="80px" size="small">
            <el-form-item label="题材">
              <el-select v-model="params.genre" placeholder="选择题材">
                <el-option v-for="g in genres" :key="g" :label="g" :value="g" />
              </el-select>
            </el-form-item>
            <el-form-item label="风格">
              <el-select v-model="params.style" placeholder="选择风格" clearable>
                <el-option v-for="s in styles" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
            <el-form-item label="创作提示">
              <el-input v-model="params.prompt" type="textarea" :rows="3" placeholder="描述你想要的故事方向" />
            </el-form-item>
            <el-form-item label="目标卷数">
              <el-input-number v-model="params.volumeCount" :min="1" :max="20" />
            </el-form-item>
            <el-form-item label="每卷章节">
              <el-input-number v-model="params.chaptersPerVolume" :min="3" :max="50" />
            </el-form-item>
            <el-form-item label="每章字数">
              <el-input-number v-model="params.wordsPerChapter" :min="1000" :max="10000" :step="500" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="startGenerate" :disabled="!params.genre || !params.prompt">
                开始生成
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="chat-panel">
          <AgentChat
            :messages="chat.messages.value"
            :is-streaming="chat.isStreaming.value"
            :current-stream-text="chat.currentStreamText.value"
            @send="chat.sendMessage"
            @clean="chat.clearHistory"
          />
        </div>
      </el-col>

      <!-- 右侧：实时预览 -->
      <el-col :span="14">
        <div class="preview-panel">
          <h4>生成预览</h4>
          <div v-if="chat.isStreaming.value" class="streaming-text">
            <MarkdownRenderer :content="chat.currentStreamText.value" />
          </div>
          <el-empty v-else description="等待生成..." />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute } from 'vue-router'
import { useAgentChat } from '@/composables/useAgentChat'
import AgentChat from '@/components/AgentChat.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const route = useRoute()
const projectId = route.params.projectId as string

const chat = useAgentChat('novel', projectId)
chat.connect()

const genres = ['都市', '玄幻', '科幻', '言情', '悬疑', '无限流', '系统流', '末日生存', '历史架空', '游戏竞技']
const styles = ['轻松搞笑', '热血燃向', '暗黑压抑', '温馨治愈', '悬疑烧脑']

const params = reactive({
  genre: '',
  style: '',
  prompt: '',
  volumeCount: 3,
  chaptersPerVolume: 10,
  wordsPerChapter: 3000,
})

function startGenerate() {
  const msg = `请开始生成小说。题材：${params.genre}，风格：${params.style || '默认'}，提示：${params.prompt}，目标${params.volumeCount}卷，每卷${params.chaptersPerVolume}章，每章${params.wordsPerChapter}字。`
  chat.sendMessage(msg)
}
</script>

<style scoped>
.novel-generate { height: calc(100vh - 100px); }
.params-panel { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.params-panel h4 { margin: 0 0 12px; }
.chat-panel { background: #fff; border-radius: 8px; height: 350px; overflow: hidden; }
.preview-panel { background: #fff; border-radius: 8px; padding: 16px; min-height: 600px; }
.preview-panel h4 { margin: 0 0 12px; }
.streaming-text { line-height: 1.8; font-size: 15px; max-height: 560px; overflow-y: auto; }
</style>
