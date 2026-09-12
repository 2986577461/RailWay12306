<template>
  <div class="page">
    <h2>我的订单</h2>
    <p v-if="error" class="empty">{{ error }}</p>
    <p v-else-if="!items.length" class="empty">暂无订单</p>
    <article v-for="item in items" :key="item.orderNo" class="card row">
      <RouterLink class="info" :to="`/orders/${item.orderNo}`">
        <b>{{ item.trainNo || '车次' }} {{ item.fromStationName }} → {{ item.toStationName }}</b>
        <p>{{ item.orderNo }} · {{ item.createdAt }}</p>
      </RouterLink>
      <div class="meta">
        <strong>¥{{ money(item.totalAmount) }}</strong>
        <span :class="['tag', tagClass(item.status)]">{{ item.statusText }}</span>
        <RouterLink v-if="item.status === 1" class="btn btn-primary btn-sm" :to="`/orders/${item.orderNo}`">去支付</RouterLink>
        <button
          v-else-if="item.status === 2"
          class="btn btn-ghost btn-sm"
          type="button"
          :disabled="refunding === item.orderNo"
          @click="refund(item)"
        >{{ refunding === item.orderNo ? '退票中...' : '退票' }}</button>
      </div>
    </article>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listOrders } from '../api/order'
import { refundPayment } from '../api/payment'

const items = ref([])
const error = ref('')
const refunding = ref('')
const money = (value) => Number(value || 0).toFixed(2)
const tagClass = (status) => ({ 1: 'pending', 2: 'paid', 3: 'cancel', 4: 'done', 5: 'refund' }[status] || '')

async function load() {
  items.value = await listOrders()
}

async function refund(item) {
  if (!confirm(`确认退票 ${item.orderNo}？退款后座位将释放。`)) return
  refunding.value = item.orderNo
  error.value = ''
  try {
    await refundPayment(item.orderNo)
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    refunding.value = ''
  }
}

onMounted(async () => {
  try {
    await load()
  } catch (e) {
    error.value = e.message
  }
})
</script>

<style scoped>
.row { display: flex; justify-content: space-between; gap: 16px; padding: 18px 20px; margin-bottom: 12px; }
.info { flex: 1; }
.row p { margin: 8px 0 0; color: #8a93a0; }
.meta { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.btn-sm { padding: 6px 12px; font-size: 13px; }
.tag { font-size: 12px; padding: 2px 8px; border-radius: 999px; }
.tag.pending { color: #c56a00; background: #fff3e0; }
.tag.paid { color: #1a73c7; background: #e8f1fb; }
.tag.cancel, .tag.refund { color: #8a93a0; background: #f3f5f7; }
.tag.done { color: #0f8a4b; background: #e7f7ee; }
</style>
