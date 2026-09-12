<template>
  <div class="page narrow">
    <form class="card form" @submit.prevent="onSubmit">
      <h2>注册</h2>
      <div class="field">
        <label>手机号</label>
        <input v-model.trim="form.phone" class="input" maxlength="11" />
      </div>
      <div class="field">
        <label>短信验证码</label>
        <div class="row">
          <input v-model.trim="form.smsCode" class="input" maxlength="6" />
          <button class="btn btn-ghost" type="button" :disabled="countdown > 0" @click="sendCode">
            {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
          </button>
        </div>
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="form.password" class="input" type="password" placeholder="6-20位密码" />
      </div>
      <p v-if="hint" class="hint">{{ hint }}</p>
      <p v-if="error" class="error">{{ error }}</p>
      <button class="btn btn-primary" :disabled="loading" type="submit">{{ loading ? '提交中...' : '注册并登录' }}</button>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, sendSmsCode } from '../api/user'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const hint = ref('')
const countdown = ref(0)
const form = reactive({ phone: '', password: '', smsCode: '' })

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
    auth.setSession(await register(form))
    router.replace('/')
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
.row { display: grid; grid-template-columns: 1fr auto; gap: 8px; }
.hint { margin: 0; color: #1a73c7; }
.error { margin: 0; color: #e21c21; }
</style>
