<template>
  <div class="project-list">
    <div class="page-header">
      <h2>我的项目</h2>
      <el-button type="primary" @click="showCreate = true">新建项目</el-button>
    </div>

    <div v-loading="loading" class="project-grid">
      <div
        v-for="p in projects"
        :key="p.id"
        class="project-card"
        @click="$router.push(`/projects/${p.id}/novel`)"
      >
        <div class="card-header">
          <span class="card-title">{{ p.name }}</span>
          <el-dropdown @command="(cmd: string) => handleAction(cmd, p)" trigger="click" @click.stop>
            <el-icon class="more-icon"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="delete">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <p class="card-intro">{{ p.intro || '暂无简介' }}</p>
        <div class="card-tags">
          <el-tag v-if="p.type" size="small">{{ p.type }}</el-tag>
          <el-tag v-if="p.artStyle" size="small" type="success">{{ p.artStyle }}</el-tag>
          <el-tag v-if="p.videoRatio" size="small" type="info">{{ p.videoRatio }}</el-tag>
        </div>
        <div class="card-time">{{ formatDate(p.createTime) }}</div>
      </div>

      <div v-if="!loading && projects.length === 0" class="empty-tip">
        <el-empty description="还没有项目，点击右上角创建一个吧" />
      </div>
    </div>

    <!-- 新建项目弹窗 -->
    <el-dialog v-model="showCreate" title="新建项目" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.intro" type="textarea" :rows="3" placeholder="项目简介（可选）" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="选择类型" clearable>
            <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="画风">
          <el-select v-model="form.artStyle" placeholder="选择画风" clearable>
            <el-option v-for="s in styleOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="画幅比">
          <el-select v-model="form.videoRatio" placeholder="选择画幅比" clearable>
            <el-option v-for="r in ratioOptions" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getProjects, createProject, deleteProject } from '@/api/project'
import { formatDate } from '@/utils/format'
import { ElMessageBox, ElMessage } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'
import type { Project, ProjectForm } from '@/types/api'
import type { FormInstance, FormRules } from 'element-plus'

const projects = ref<Project[]>([])
const loading = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const formRef = ref<FormInstance>()

const typeOptions = ['都市', '玄幻', '科幻', '言情', '悬疑', '无限流', '系统流', '末日生存', '历史架空', '游戏竞技']
const styleOptions = ['CG', '二次元', '水墨', '写实', '赛博朋克']
const ratioOptions = ['16:9', '9:16', '1:1', '4:3']

const form = reactive<ProjectForm>({
  name: '',
  intro: '',
  type: '',
  artStyle: '',
  videoRatio: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
}

async function loadProjects() {
  loading.value = true
  try {
    const res = await getProjects()
    projects.value = res.records
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await createProject(form)
    ElMessage.success('创建成功')
    showCreate.value = false
    resetForm()
    loadProjects()
  } finally {
    creating.value = false
  }
}

async function handleAction(cmd: string, project: Project) {
  if (cmd === 'delete') {
    await ElMessageBox.confirm(`确定删除项目「${project.name}」？此操作不可恢复。`, '确认删除', { type: 'warning' })
    await deleteProject(project.id)
    ElMessage.success('删除成功')
    loadProjects()
  }
}

function resetForm() {
  form.name = ''
  form.intro = ''
  form.type = ''
  form.artStyle = ''
  form.videoRatio = ''
}

onMounted(loadProjects)
</script>

<style scoped>
.project-list { padding: 0; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-header h2 { margin: 0; font-size: 20px; }
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.project-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: box-shadow 0.2s;
  border: 1px solid #e8e8e8;
}
.project-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-title { font-size: 16px; font-weight: 600; }
.more-icon { font-size: 18px; color: #999; cursor: pointer; }
.card-intro { color: #666; font-size: 13px; margin: 0 0 12px; line-height: 1.5; }
.card-tags { display: flex; gap: 6px; margin-bottom: 12px; }
.card-time { color: #999; font-size: 12px; }
.empty-tip { grid-column: 1 / -1; }
</style>
