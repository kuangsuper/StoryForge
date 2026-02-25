<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo" @click="$router.push('/projects')">
        <span v-if="!isCollapsed" class="logo-text">Toonflow</span>
        <span v-else class="logo-text">TF</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        router
        class="aside-menu"
        background-color="#1d1e2c"
        text-color="#a3a6b4"
        active-text-color="#409eff"
      >
        <el-menu-item index="/projects">
          <el-icon><Folder /></el-icon>
          <template #title>项目列表</template>
        </el-menu-item>
        <el-menu-item index="/materials">
          <el-icon><Picture /></el-icon>
          <template #title>素材库</template>
        </el-menu-item>
        <el-menu-item index="/tasks">
          <el-icon><List /></el-icon>
          <template #title>任务中心</template>
        </el-menu-item>
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>监控仪表盘</template>
        </el-menu-item>
        <el-sub-menu index="/settings">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/settings/models">AI模型配置</el-menu-item>
          <el-menu-item index="/settings/prompts">Prompt管理</el-menu-item>
          <el-menu-item index="/settings/users">用户管理</el-menu-item>
          <el-menu-item index="/settings/system">系统设置</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-header class="layout-header">
        <el-icon class="collapse-btn" @click="isCollapsed = !isCollapsed">
          <Expand v-if="isCollapsed" />
          <Fold v-else />
        </el-icon>
        <div class="header-right">
          <span class="user-name">{{ authStore.userName }}</span>
          <el-dropdown @command="handleCommand">
            <el-avatar :size="32">{{ authStore.userName?.charAt(0) }}</el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { Folder, Picture, List, DataLine, Setting, Expand, Fold } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapsed = ref(false)

const activeMenu = computed(() => route.path)

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}
.layout-aside {
  background: #1d1e2c;
  transition: width 0.3s;
  overflow: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.logo-text {
  color: #fff;
  font-size: 20px;
  font-weight: bold;
}
.aside-menu {
  border-right: none;
}
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 20px;
  height: 60px;
}
.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name {
  font-size: 14px;
  color: #333;
}
.layout-main {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}
</style>
