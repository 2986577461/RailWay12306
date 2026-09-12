<template>
  <div class="page">
    <p v-if="error" class="empty">{{ error }}</p>
    <section v-else-if="order" class="card detail">
      <h2>订单 {{ order.orderNo }}</h2>
      <p>状态：{{ statusText(order.status) }}</p>
      <p>车次运行 ID：{{ order.trainRunId }}</p>
      <p>金额：¥{{ order.totalAmount ?? 0 }}</p>
      <p>创建时间：{{ order.createdAt }}</p>
      <p class="tip">支付功能暂未开放，订单提交后进入排队出票。</p>
      <RouterLink class="btn btn-ghost" to="/orders">返回订单列表</RouterLink>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getOrder } from '../api/order'

const route = useRoute()
const order = ref(null)
const error = ref('')
const statusText = (status) => ({
  QUEUED: '排队出票中',
  WAIT_PAY: '待支付',
  PAID: '已支付',
  CANCELLED: '已取消',
  FAILED: '出票失败'
}[status] || status)

onMounted(async () => {
  try {
    order.value = await getOrder(route.params.orderNo)
  } catch (e) {
    error.value = e.message
  }
})
</script>

<style scoped>
.detail { padding: 24px; display: grid; gap: 8px; }
.tip { color: #8a93a0; }
</style>
