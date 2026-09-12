<template>
  <div class="page">
    <h2>我的订单</h2>
    <p v-if="error" class="empty">{{ error }}</p>
    <p v-else-if="!items.length" class="empty">暂无订单</p>
    <RouterLink v-for="item in items" :key="item.orderNo" class="card row" :to="`/orders/${item.orderNo}`">
      <div>
        <b>{{ item.orderNo }}</b>
        <p>{{ item.createdAt }}</p>
      </div>
      <strong>{{ statusText(item.status) }}</strong>
    </RouterLink>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listOrders } from '../api/order'

const items = ref([])
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
    items.value = await listOrders()
  } catch (e) {
    error.value = e.message
  }
})
</script>

<style scoped>
.row { display: flex; justify-content: space-between; padding: 18px 20px; margin-bottom: 12px; }
.row p { margin: 8px 0 0; color: #8a93a0; }
strong { color: #e21c21; }
</style>
