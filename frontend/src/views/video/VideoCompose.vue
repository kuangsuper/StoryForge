<template>
  <div class="video-compose">
    <div class="page-header">
      <h3>视频合成</h3>
      <el-button type="primary" @click="handleCompose" :loading="composing">提交合成</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="合成配置">
          <el-form :model="config" label-width="100px" size="small" v-loading="loadingConfig">
            <el-form-item label="转场效果">
              <el-select v-model="config.transition">
                <el-option label="无" value="none" />
                <el-option label="淡入淡出" value="fadeInOut" />
                <el-option label="交叉溶解" value="crossDissolve" />
                <el-option label="黑屏" value="blackScreen" />
              </el-select>
            </el-form-item>
            <el-form-item label="转场时长(ms)">
              <el-input-number v-model="config.transitionDuration" :min="0" :max="3000" :step="100" />
            </el-form-item>
            <el-form-item label="BGM音量">
              <el-slider v-model="config.bgmVolume" :max="100" />
            </el-form-item>
            <el-form-item label="字幕">
              <el-switch v-model="config.subtitleEnabled" />
            </el-form-item>
            <el-form-item label="水印文字">
              <el-input v-model="config.watermarkText" />
            </el-form-item>
            <el-form-item label="片头文字">
              <el-input v-model="config.introText" />
            </el-form-item>
            <el-form-item label="片尾文字">
              <el-input v-model="config.outroText" />
            </el-form-item>
            <el-form-item label="分辨率">
              <el-select v-model="config.outputResolution">
                <el-option label="720p" value="720p" />
                <el-option label="1080p" value="1080p" />
                <el-option label="4K" value="4k" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="handleSaveConfig" :loading="savingConfig">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card header="合成记录">
          <el-table :data="composeList" v-loading="loadingList" size="small">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'success' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="视频">
              <template #default="{ row }">
                <a v-if="row.outputUrl" :href="row.outputUrl" target="_blank">下载</a>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button v-if="row.status === 'failed'" size="small" @click="handleRetry(row.id)">重试</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getComposeConfig, updateComposeConfig, submitCompose, getComposeList, retryCompose } from '@/api/videoCompose'
import { ElMessage } from 'element-plus'

const route = useRoute()
const projectId = Number(route.params.projectId)
const scriptId = Number(route.params.scriptId)

const config = reactive({
  transition: 'fadeInOut',
  transitionDuration: 500,
  bgmVolume: 30,
  subtitleEnabled: true,
  watermarkText: '',
  introText: '',
  outroText: '',
  outputResolution: '1080p',
})

const composeList = ref<any[]>([])
const loadingConfig = ref(false)
const loadingList = ref(false)
const composing = ref(false)
const savingConfig = ref(false)

async function loadConfig() {
  loadingConfig.value = true
  try {
    const res = await getComposeConfig(projectId)
    if (res) Object.assign(config, res)
  } finally {
    loadingConfig.value = false
  }
}

async function loadList() {
  loadingList.value = true
  try {
    composeList.value = await getComposeList(projectId)
  } finally {
    loadingList.value = false
  }
}

async function handleSaveConfig() {
  savingConfig.value = true
  try {
    await updateComposeConfig(projectId, config)
    ElMessage.success('配置已保存')
  } finally {
    savingConfig.value = false
  }
}

async function handleCompose() {
  composing.value = true
  try {
    await submitCompose(projectId, { scriptId })
    ElMessage.success('合成任务已提交')
    setTimeout(loadList, 3000)
  } finally {
    composing.value = false
  }
}

async function handleRetry(id: number) {
  await retryCompose(projectId, id)
  ElMessage.success('重试已提交')
  setTimeout(loadList, 3000)
}

onMounted(() => { loadConfig(); loadList() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
