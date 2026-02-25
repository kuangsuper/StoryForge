import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/storage'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { layout: 'blank', public: true },
    },
    {
      path: '/projects',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', component: () => import('@/views/project/ProjectList.vue') },
      ],
    },
    {
      path: '/projects/:projectId',
      component: () => import('@/views/project/ProjectWorkbench.vue'),
      children: [
        { path: 'novel', component: () => import('@/views/novel/NovelManage.vue'), meta: { title: '小说管理' } },
        { path: 'novel/generate', component: () => import('@/views/novel/NovelGenerate.vue'), meta: { title: 'AI小说生成' } },
        { path: 'storyline', component: () => import('@/views/storyline/StorylineView.vue'), meta: { title: '故事线' } },
        { path: 'outline', component: () => import('@/views/outline/OutlineList.vue'), meta: { title: '大纲管理' } },
        { path: 'outline/agent', component: () => import('@/views/outline/OutlineAgent.vue'), meta: { title: 'Agent对话' } },
        { path: 'assets', component: () => import('@/views/asset/AssetManage.vue'), meta: { title: '资产管理' } },
        { path: 'scripts', component: () => import('@/views/script/ScriptManage.vue'), meta: { title: '剧本管理' } },
        { path: 'storyboard/:scriptId', component: () => import('@/views/storyboard/StoryboardWorkbench.vue'), meta: { title: '分镜工作台' } },
        { path: 'video/:scriptId', component: () => import('@/views/video/VideoGenerate.vue'), meta: { title: '视频生成' } },
        { path: 'video/:scriptId/compose', component: () => import('@/views/video/VideoCompose.vue'), meta: { title: '视频合成' } },
        { path: 'tts/:scriptId', component: () => import('@/views/tts/TtsView.vue'), meta: { title: 'AI配音' } },
        { path: 'pipeline', component: () => import('@/views/pipeline/PipelineView.vue'), meta: { title: '全自动流水线' } },
        { path: '', redirect: (to) => `/projects/${to.params.projectId}/novel` },
      ],
    },
    {
      path: '/settings',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: 'models', component: () => import('@/views/settings/ModelConfig.vue'), meta: { title: 'AI模型配置' } },
        { path: 'prompts', component: () => import('@/views/settings/PromptManage.vue'), meta: { title: 'Prompt管理' } },
        { path: 'users', component: () => import('@/views/settings/UserManage.vue'), meta: { title: '用户管理' } },
        { path: 'system', component: () => import('@/views/settings/SystemSetting.vue'), meta: { title: '系统设置' } },
      ],
    },
    {
      path: '/materials',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', component: () => import('@/views/material/MaterialLibrary.vue'), meta: { title: '素材库' } },
      ],
    },
    {
      path: '/tasks',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', component: () => import('@/views/task/TaskCenter.vue'), meta: { title: '任务中心' } },
      ],
    },
    {
      path: '/dashboard',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '', component: () => import('@/views/monitor/Dashboard.vue'), meta: { title: '监控仪表盘' } },
      ],
    },
    { path: '/', redirect: '/projects' },
    { path: '/:pathMatch(.*)*', redirect: '/projects' },
  ],
})

router.beforeEach((to, _from, next) => {
  if (to.meta.public) return next()
  if (!getToken()) return next('/login')
  next()
})

export default router
