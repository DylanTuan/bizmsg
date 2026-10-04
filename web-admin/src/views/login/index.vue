<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { APP_TITLE } from '@/constants'
import { HOME_PATH } from '@/router/routes'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    // 被路由守卫拦下来的地址优先，否则回工作台
    const redirect = route.query.redirect
    await router.replace(typeof redirect === 'string' && redirect ? redirect : HOME_PATH)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <el-card class="login__card" shadow="always">
      <div class="login__brand">
        <img class="login__logo" src="/favicon.svg" alt="logo" />
        <div>
          <h1 class="login__title">{{ APP_TITLE }}</h1>
          <p class="login__subtitle">XML 报文生成与上传 · 管理端</p>
        </div>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="'User'" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="'Lock'" show-password />
        </el-form-item>
        <el-form-item>
          <el-button class="login__submit" type="primary" :loading="loading" @click="handleSubmit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert type="info" :closable="false" show-icon>
        <template #title>演示账号 admin / 123456</template>
        <div class="login__tip">
          后端尚未提供认证服务，此处为前端模拟登录；接入真实接口后只需替换
          <code>src/api/auth.ts</code> 中的实现。
        </div>
      </el-alert>
    </el-card>
  </div>
</template>

<style scoped>
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #1f2d3d 0%, #1f6feb 100%);
}

.login__card {
  width: 420px;
  padding: 8px 12px;
  border-radius: 10px;
}

.login__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
}

.login__logo {
  width: 42px;
  height: 42px;
}

.login__title {
  margin: 0;
  font-size: 20px;
  color: #1f2d3d;
}

.login__subtitle {
  margin: 4px 0 0;
  font-size: 12px;
  color: #909399;
}

.login__submit {
  width: 100%;
}

.login__tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
}
</style>
