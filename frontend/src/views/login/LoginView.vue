<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">Toonflow</h2>
      <p class="login-subtitle">AI 驱动的短剧内容生产平台</p>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="name">
          <el-input v-model="form.name" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="captcha">
          <div class="captcha-row">
            <el-input v-model="form.captcha" placeholder="验证码" size="large" class="captcha-input" />
            <img
              v-if="captchaImg"
              :src="'data:image/png;base64,' + captchaImg"
              class="captcha-img"
              @click="loadCaptcha"
              alt="验证码"
            />
            <el-button v-else size="large" @click="loadCaptcha">获取验证码</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" native-type="submit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getCaptcha } from '@/api/auth'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaImg = ref('')
const captchaId = ref('')

const form = reactive({
  name: '',
  password: '',
  captcha: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    captchaId.value = res.captchaId
    captchaImg.value = res.image
  } catch {
    // ignore
  }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await authStore.login({ ...form, captchaId: captchaId.value })
    router.push('/projects')
  } catch {
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d1e2c 0%, #2d3a5c 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
.login-title {
  text-align: center;
  font-size: 28px;
  color: #1d1e2c;
  margin: 0 0 8px;
}
.login-subtitle {
  text-align: center;
  color: #999;
  margin: 0 0 32px;
  font-size: 14px;
}
.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.captcha-input {
  flex: 1;
}
.captcha-img {
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}
.login-btn {
  width: 100%;
}
</style>
