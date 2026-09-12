<template>
  <div class="page">
    <p v-if="error" class="empty">{{ error }}</p>
    <section v-else-if="order" class="card detail">
      <div class="head">
        <h2>{{ order.trainNo || '订单详情' }}</h2>
        <span :class="['tag', tagClass(order.status)]">{{ order.statusText }}</span>
      </div>
      <p>{{ order.fromStationName }} → {{ order.toStationName }}</p>
      <p>订单号：{{ order.orderNo }}</p>
      <p>金额：¥{{ money(order.totalAmount) }}</p>
      <p>创建时间：{{ order.createdAt }}</p>
      <p v-if="order.expireAt">支付截止：{{ order.expireAt }}</p>

      <div v-if="order.status === 1" class="pay">
        <p v-if="payError" class="error">{{ payError }}</p>
        <button class="btn btn-primary" :disabled="opening" type="button" @click="openCashier">
          {{ opening ? '正在拉起收银台...' : '去支付' }}
        </button>
      </div>
      <div v-else-if="order.status === 2" class="pay">
        <p class="ok">支付成功，订单已进入已支付状态。</p>
        <p v-if="payError" class="error">{{ payError }}</p>
        <button class="btn btn-ghost" :disabled="refunding" type="button" @click="refund">
          {{ refunding ? '退票中...' : '退票' }}
        </button>
      </div>
      <p v-else-if="order.status === 5" class="tip">该订单已退款，座位已释放。</p>
      <RouterLink class="btn btn-ghost" to="/orders">返回订单列表</RouterLink>
    </section>

    <div v-if="cashierOpen" class="mask" @click.self="closeCashier">
      <section class="card cashier">
        <h3>铁路 12306 收银台</h3>
        <p class="amount">¥{{ money(payment.amount) }}</p>
        <div class="qr" aria-hidden="true">
          <span v-for="n in 36" :key="n" :class="{ on: n % 3 === 0 || n % 7 === 0 }"></span>
        </div>
        <p v-if="countdown > 0" class="wait">等待支付... {{ countdown }}s</p>
        <p v-else class="wait ready">可确认支付</p>
        <p v-if="payError" class="error">{{ payError }}</p>
        <button class="btn btn-primary" :disabled="countdown > 0 || confirming" type="button" @click="confirmPay">
          {{ confirming ? '正在回调...' : '模拟支付成功' }}
        </button>
        <button class="btn btn-ghost" type="button" @click="closeCashier">取消</button>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getOrder } from '../api/order'
import { createPayment, mockNotify, refundPayment } from '../api/payment'

const route = useRoute()
const order = ref(null)
const error = ref('')
const payError = ref('')
const opening = ref(false)
const cashierOpen = ref(false)
const confirming = ref(false)
const refunding = ref(false)
const countdown = ref(0)
const payment = reactive({ paymentNo: '', amount: null, mockSign: '' })

let timer = 0
const COUNTDOWN_SECONDS = 4

const money = (value) => Number(value || 0).toFixed(2)
const tagClass = (status) => ({ 1: 'pending', 2: 'paid', 3: 'cancel', 4: 'done', 5: 'refund' }[status] || '')

async function load() {
  order.value = await getOrder(route.params.orderNo)
}

function startCountdown() {
  clearInterval(timer)
  countdown.value = COUNTDOWN_SECONDS
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function openCashier() {
  opening.value = true
  payError.value = ''
  try {
    const created = await createPayment(order.value.orderNo)
    payment.paymentNo = created.paymentNo
    payment.amount = created.amount
    payment.mockSign = created.mockSign
    cashierOpen.value = true
    startCountdown()
  } catch (e) {
    payError.value = e.message
  } finally {
    opening.value = false
  }
}

async function confirmPay() {
  confirming.value = true
  payError.value = ''
  try {
    await mockNotify(order.value.orderNo, {
      paymentNo: payment.paymentNo,
      amount: payment.amount,
      sign: payment.mockSign
    })
    cashierOpen.value = false
    await load()
  } catch (e) {
    payError.value = e.message
  } finally {
    confirming.value = false
  }
}

function closeCashier() {
  cashierOpen.value = false
  clearInterval(timer)
}

async function refund() {
  if (!confirm(`确认退票 ${order.value.orderNo}？退款后座位将释放。`)) return
  refunding.value = true
  payError.value = ''
  try {
    await refundPayment(order.value.orderNo)
    await load()
  } catch (e) {
    payError.value = e.message
  } finally {
    refunding.value = false
  }
}

onMounted(async () => {
  try {
    await load()
  } catch (e) {
    error.value = e.message
  }
})

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.detail { padding: 24px; display: grid; gap: 8px; }
.head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.pay { display: grid; gap: 10px; margin: 8px 0 4px; }
.tip, .wait { color: #8a93a0; margin: 0; }
.wait.ready { color: #0f8a4b; }
.error { color: #e21c21; margin: 0; }
.ok { color: #0f8a4b; }
.tag { font-size: 12px; padding: 2px 8px; border-radius: 999px; background: #f3f5f7; }
.tag.pending { color: #c56a00; background: #fff3e0; }
.tag.paid { color: #1a73c7; background: #e8f1fb; }
.tag.cancel, .tag.refund { color: #8a93a0; background: #f3f5f7; }
.tag.done { color: #0f8a4b; background: #e7f7ee; }
.cashier {
  width: min(360px, 100%);
  padding: 24px;
  display: grid;
  gap: 12px;
  text-align: center;
}
.amount { margin: 0; font-size: 32px; font-weight: 700; color: #e21c21; }
.qr {
  width: 180px;
  height: 180px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 4px;
  padding: 12px;
  background: #111;
}
.qr span { background: #111; }
.qr span.on { background: #fff; }
</style>
