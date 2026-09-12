<template>
  <div class="page narrow">
    <section class="card profile">
      <h2>个人中心</h2>
      <p>手机号：{{ auth.user?.phone || '-' }}</p>
      <p>姓名：{{ auth.user?.realName || '未实名' }}</p>
      <div class="ops">
        <RouterLink class="btn btn-ghost" to="/passengers">乘车人</RouterLink>
        <RouterLink class="btn btn-ghost" to="/orders">我的订单</RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
onMounted(() => auth.refreshMe().catch(() => {}))
</script>

<style scoped>
.narrow { width: min(520px, calc(100% - 32px)); }
.profile { padding: 24px; display: grid; gap: 10px; }
.ops { display: flex; gap: 12px; margin-top: 8px; }
</style>
