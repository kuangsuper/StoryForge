<template>
  <div class="dashboard">
    <div class="page-header">
      <h3>监控仪表盘</h3>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ overview.projectCount || 0 }}</div>
          <div class="stat-label">项目总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ overview.chapterCount || 0 }}</div>
          <div class="stat-label">章节总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ overview.videoCount || 0 }}</div>
          <div class="stat-label">视频总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ formatWordCount(overview.totalWords || 0) }}</div>
          <div class="stat-label">总字数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card header="模型调用统计" v-loading="loadingStats">
          <el-table :data="modelStats" size="small">
            <el-table-column prop="model" label="模型" />
            <el-table-column prop="totalCalls" label="调用次数" width="100" />
            <el-table-column prop="successRate" label="成功率" width="80">
              <template #default="{ row }">{{ row.successRate }}%</template>
            </el-table-column>
            <el-table-column prop="avgDuration" label="平均耗时" width="100">
              <template #default="{ row }">{{ row.avgDuration }}ms</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="告警列表" v-loading="loadingAlerts">
          <el-table :data="alerts" size="small">
            <el-table-column prop="level" label="级别" width="60">
              <template #default="{ row }">
                <el-tag :type="row.level === 'error' ? 'danger' : 'warning'" size="small">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="内容" show-overflow-tooltip />
            <el-table-column prop="time" label="时间" width="160" />
          </el-table>
          <el-empty v-if="alerts.length === 0" description="暂无告警" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getStats, getModelUsage } from '@/api/monitor'
import { formatWordCount } from '@/utils/format'

const overview = reactive({ projectCount: 0, chapterCount: 0, videoCount: 0, totalWords: 0 })
const modelStats = ref<any[]>([])
const alerts = ref<any[]>([])
const loadingStats = ref(false)
const loadingAlerts = ref(false)

async function load() {
  try {
    const res = await getStats()
    if (res) Object.assign(overview, res)
  } catch { /* ignore */ }

  loadingStats.value = true
  try {
    const res = await getModelUsage()
    modelStats.value = res?.models || []
  } catch { /* ignore */ } finally { loadingStats.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.stat-card { text-align: center; padding: 20px 0; }
.stat-value { font-size: 32px; font-weight: bold; color: #409eff; }
.stat-label { font-size: 14px; color: #999; margin-top: 8px; }
</style>
