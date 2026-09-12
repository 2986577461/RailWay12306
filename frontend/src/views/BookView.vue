<template>
  <div class="page">
    <p v-if="!train" class="empty">没有选中的车次，请先去查询余票。</p>
    <template v-else>
      <section class="card summary">
        <div>
          <b>{{ train.trainNo }}</b>
          <span>{{ train.date }} · {{ train.seatTypeName }} · ¥{{ train.price ?? "--" }}</span>
        </div>
        <div class="path">
          <strong>{{ train.departTime }} {{ train.fromStationName }}</strong>
          <span>{{ train.duration }}</span>
          <strong>{{ train.arriveTime }} {{ train.toStationName }}</strong>
        </div>
      </section>

      <section class="card block">
        <div class="head">
          <h3>选择乘车人</h3>
          <RouterLink to="/passengers">管理乘车人</RouterLink>
        </div>
        <p v-if="!passengers.length" class="empty">请先添加乘车人</p>
        <label v-for="item in passengers" :key="item.id" class="passenger">
          <input v-model="selectedIds" type="checkbox" :value="item.id" />
          <span>{{ item.name }} · {{ item.idCardNo }}</span>
        </label>
      </section>

      <p v-if="error" class="error">{{ error }}</p>
      <button class="btn btn-primary" :disabled="loading || !selectedIds.length" type="button" @click="submit">
        {{ loading ? '提交中...' : '提交购票请求' }}
      </button>
      <p class="tip">下单后进入排队出票，支付功能暂未开放。</p>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listPassengers } from '../api/passenger'
import { createOrder } from '../api/order'

const router = useRouter()
const train = ref(null)
const passengers = ref([])
const selectedIds = ref([])
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  const raw = sessionStorage.getItem('railway.selectedTrain')
  train.value = raw ? JSON.parse(raw) : null
  try {
    passengers.value = await listPassengers()
  } catch (e) {
    error.value = e.message
  }
})

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const result = await createOrder({
      trainRunId: train.value.trainRunId,
      seatTypeId: train.value.seatTypeId,
      fromStationId: train.value.fromStationId,
      toStationId: train.value.toStationId,
      fromSeq: train.value.fromSeq,
      toSeq: train.value.toSeq,
      passengerIds: selectedIds.value
    })
    router.replace({ name: 'order-detail', params: { orderNo: result.orderNo } })
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.summary, .block { padding: 20px; margin-bottom: 16px; }
.path { display: flex; gap: 16px; margin-top: 10px; color: #5b6573; }
.head { display: flex; justify-content: space-between; }
.passenger { display: flex; gap: 8px; padding: 10px 0; }
.error { color: #e21c21; }
.tip { color: #8a93a0; }
</style>
