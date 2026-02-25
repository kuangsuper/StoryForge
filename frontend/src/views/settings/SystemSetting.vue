<template>
  <div class="system-setting">
    <div class="page-header">
      <h3>系统设置</h3>
    </div>
    <el-card v-loading="loading">
      <el-form :model="settings" label-width="120px">
        <el-form-item label="站点名称">
          <el-input v-model="settings.siteName" />
        </el-form-item>
        <el-form-item label="默认画风">
          <el-input v-model="settings.defaultArtStyle" />
        </el-form-item>
        <el-form-item label="默认画幅比">
          <el-select v-model="settings.defaultVideoRatio">
            <el-option label="16:9" value="16:9" />
            <el-option label="9:16" value="9:16" />
            <el-option label="1:1" value="1:1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getSettings, updateSettings } from '@/api/setting'
import { ElMessage } from 'element-plus'

const settings = reactive({ siteName: 'Toonflow', defaultArtStyle: 'CG', defaultVideoRatio: '16:9' })
const loading = ref(false)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getSettings()
    if (res) Object.assign(settings, res)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateSettings(settings)
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
