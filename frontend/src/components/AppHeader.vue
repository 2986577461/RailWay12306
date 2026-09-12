<template>
  <header class="header">
    <div class="header-inner">
      <RouterLink class="brand" to="/">
        <span class="logo">12306</span>
        <span class="brand-name">铁路客运</span>
      </RouterLink>
      <nav class="nav">
        <RouterLink to="/">车票查询</RouterLink>
        <RouterLink to="/orders">我的订单</RouterLink>
        <RouterLink to="/passengers">乘车人</RouterLink>
        <RouterLink to="/admin">运营后台</RouterLink>
      </nav>
      <div class="user">
        <template v-if="auth.isLoggedIn">
          <RouterLink class="phone" to="/profile">{{ auth.user?.phone }}</RouterLink>
          <button class="btn-link" type="button" @click="onLogout">退出</button>
        </template>
        <template v-else>
          <RouterLink to="/login">登录</RouterLink>
          <RouterLink class="register" to="/register">注册</RouterLink>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

function onLogout() {
  auth.logout()
  router.push('/')
}
</script>

<style scoped>
.header {
  background: linear-gradient(90deg, #b31217, #e21c21 45%, #c8161d);
  color: #fff;
}
.header-inner {
  width: min(1120px, calc(100% - 32px));
  margin: 0 auto;
  min-height: 64px;
  display: flex;
  align-items: center;
  gap: 32px;
}
.brand { display: flex; align-items: center; gap: 10px; font-weight: 700; }
.logo {
  background: #fff;
  color: #e21c21;
  border-radius: 8px;
  padding: 4px 8px;
  letter-spacing: .5px;
}
.nav { display: flex; gap: 18px; flex: 1; }
.nav a { opacity: .86; }
.nav a.router-link-active, .nav a:hover { opacity: 1; }
.user { display: flex; gap: 14px; align-items: center; }
.register, .phone { font-weight: 600; }
.btn-link { color: #fff; }
</style>
