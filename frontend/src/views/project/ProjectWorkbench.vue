<template>
  <el-container class="workbench">
    <!-- 项目侧边栏 -->
    <el-aside width="200px" class="workbench-aside">
      <div class="project-name" @click="$router.push('/projects')">
        <el-icon><ArrowLeft /></el-icon>
        <span>{{ projectStore.current?.name || '加载中...' }}</span>
      </div>
      <el-menu :default-active="activeMenu" router class="workbench-menu">
        <el-menu-item :index="`/projects/${projectId}/novel`">
          <el-icon><Document /></el-icon>
          <span>小说管理</span>
        </el-menu-item>
        <el-menu-item :index="`/projects/${projectId}/novel/generate`">
          <el-icon><MagicStick /></el-icon>
          <span>AI小说生成</span>
        </el-menu-item>
        <el-menu-item :index="`/projects/${projectId}/storyline`">
          <el-icon><Share /></el-icon>
          <span>故事线</span>
        </el-menu-item>
        <el-menu-item :index="`/projects/${projectId}/outline`">
          <el-icon><Notebook /></el-icon>
          <span>大纲管理</span>
        </el-menu-item>
        <el-menu-item :index="`/projects/${projectId}/outline/agent`">
          <el-icon><ChatDotRound /></el-icon>
          <span>Agent对话</span>
        </el-menu-item>
        <el-menu-item :index="`/projects/${projectId}/assets`">
          <el-icon><UserFilled /></el-icon>
          <span>资产管理</span>
        </el-menu-item>
        <el-menu-item :index="`/projects/${projectId}/scripts`">
          <el-icon><Tickets /></el-icon>
          <span>剧本管理</span>
        </el-menu-item>
        <el-menu-item :index="`/projects/${projectId}/pipeline`">
          <el-icon><VideoCamera /></el-icon>
          <span>全自动流水线</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 内容区 -->
    <el-main class="workbench-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Document, MagicStick, Share, Notebook,
  ChatDotRound, UserFilled, Tickets, VideoCamera
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const projectId = computed(() => route.params.projectId as string)
const activeMenu = computed(() => route.path)

async function loadProject() {
  try {
    await projectStore.load(Number(projectId.value))
  } catch {
    ElMessage.error('项目不存在')
    router.push('/projects')
  }
}

onMounted(loadProject)
watch(projectId, loadProject)
</script>

<style scoped>
.workbench { min-height: 100vh; }
.workbench-aside {
  background: #fff;
  border-right: 1px solid #e8e8e8;
  overflow-y: auto;
}
.project-name {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 1px solid #e8e8e8;
}
.project-name:hover { color: #409eff; }
.workbench-menu { border-right: none; }
.workbench-main {
  background: #f5f7fa;
  min-height: 100vh;
}
</style>
