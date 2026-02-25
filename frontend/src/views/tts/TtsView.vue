<template>
  <div class="tts-view">
    <div class="page-header">
      <h3>AI 配音</h3>
      <el-button type="primary" @click="handleGenerate" :loading="generating">生成配音</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="音色选择">
          <el-table :data="voices" v-loading="loadingVoices" size="small" highlight-current-row @current-change="selectVoice">
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="gender" label="性别" width="60" />
            <el-table-column prop="language" label="语言" width="80" />
            <el-table-column prop="provider" label="提供商" width="100" />
            <el-table-column label="试听" width="80">
              <template #default="{ row }">
                <el-button size="small" @click.stop="handlePreview(row)">试听</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card header="配音配置">
          <el-form label-width="80px" size="small">
            <el-form-item label="选中音色">
              <el-tag v-if="selectedVoice">{{ selectedVoice.name }}</el-tag>
              <span v-else class="text-gray">未选择</span>
            </el-form-item>
            <el-form-item label="试听文本">
              <el-input v-model="previewText" type="textarea" :rows="3" placeholder="输入试听文本" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card header="配音配置列表" class="mt-4" style="margin-top: 16px;">
          <el-table :data="configs" v-loading="loadingConfigs" size="small">
            <el-table-column prop="provider" label="提供商" />
            <el-table-column prop="voiceId" label="音色ID" />
            <el-table-column prop="speed" label="语速" width="60" />
            <el-table-column prop="volume" label="音量" width="60" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTtsVoices, getTtsConfigs, generateTts, previewTts } from '@/api/tts'
import { ElMessage } from 'element-plus'

const route = useRoute()
const projectId = Number(route.params.projectId)
const scriptId = Number(route.params.scriptId)

const voices = ref<any[]>([])
const configs = ref<any[]>([])
const selectedVoice = ref<any>(null)
const previewText = ref('你好，这是一段配音测试。')
const loadingVoices = ref(false)
const loadingConfigs = ref(false)
const generating = ref(false)

function selectVoice(row: any) {
  selectedVoice.value = row
}

async function handlePreview(voice: any) {
  try {
    await previewTts(projectId, { voiceId: voice.id, text: previewText.value })
    ElMessage.success('试听请求已发送')
  } catch {
    ElMessage.error('试听失败')
  }
}

async function handleGenerate() {
  generating.value = true
  try {
    await generateTts(projectId, scriptId)
    ElMessage.success('配音生成任务已提交')
  } finally {
    generating.value = false
  }
}

async function loadVoices() {
  loadingVoices.value = true
  try { voices.value = await getTtsVoices(projectId) } finally { loadingVoices.value = false }
}

async function loadConfigs() {
  loadingConfigs.value = true
  try { configs.value = await getTtsConfigs(projectId) } finally { loadingConfigs.value = false }
}

onMounted(() => { loadVoices(); loadConfigs() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.text-gray { color: #c0c4cc; }
</style>
