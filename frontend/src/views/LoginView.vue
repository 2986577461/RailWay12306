<template>
  <div class="page narrow">
    <form class="card form" @submit.prevent="onSubmit">
      <h2>登录</h2>
      <div class="tabs">
        <button type="button" :class="{ on: mode === 'password' }" @click="mode = 'password'">密码登录</button>
        <button type="button" :class="{ on: mode === 'code' }" @click="mode = 'code'">验证码登录</button>
      </div>
      <div class="field">
        <label>手机号</label>
        <input v-model.trim="form.phone" class="input" maxlength="11" placeholder="11位手机号" />
      </div>
      <div v-if="mode === 'password'" class="field">
        <label>密码</label>
        <input v-model="form.password" class="input" type="password" placeholder="6-20位密码" />
      </div>
      <div v-else class="field">
        <label>短信验证码</label>
        <div class="row">
          <input v-model.trim="form.smsCode" class="input" maxlength="6" placeholder="6位验证码" />
          <button class="btn btn-ghost" type="button" :disabled="countdown > 0" @click="sendCode">
            {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
          </button>
        </div>
      </div>
      <p v-if="hint" class="hint">{{ hint }}</p>
      <p v-if="error" class="error">{{ error }}</p>
      <button class="btn btn-primary" :disabled="loading" type="submit">{{ loading ? '登录中...' : '登录' }}</button>
      <p class="extra">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, loginByCode, sendSmsCode } from '../api/user'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const mode = ref('password')
const loading = ref(false)
const error = ref('')
const hint = ref('')
const countdown = ref(0)
const form = reactive({ phone: '', password: '', smsCode: '' })

function afterLogin(payload) {
  auth.setSession(payload)
  router.replace(route.query.redirect || '/')
}

async function sendCode() {
  error.value = ''
  try {
    const data = await sendSmsCode(form.phone)
    hint.value = data.code ? `演示验证码：${data.code}` : '验证码已发送'
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) {
    error.value = e.message
  }
}

async function onSubmit() {
  loading.value = true
  error.value = ''
  try {
    const payload = mode.value === 'password'
      ? await login({ phone: form.phone, password: form.password })
      : await loginByCode({ phone: form.phone, smsCode: form.smsCode })
    afterLogin(payload)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.narrow { width: min(460px, calc(100% - 32px)); }
.form { padding: 28px; display: grid; gap: 16px; }
.tabs { display: grid; grid-template-columns: 1fr 1fr; background: #f4f6f8; border-radius: 8px; padding: 4px; }
.tabs button { border: 0; background: transparent; height: 36px; border-radius: 6px; cursor: pointer; }
.tabs .on { background: #fff; color: #e21c21; font-weight: 700; }
.row { display: grid; grid-template-columns: 1fr auto; gap: 8px; }
.hint { margin: 0; color: #1a73c7; }
.error, .extra { margin: 0; }
.error { color: #e21c21; }
.extra { color: #5b6573; }
</style>
