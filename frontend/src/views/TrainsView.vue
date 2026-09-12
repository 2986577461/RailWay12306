<template>
  <div class="page">
    <form class="card filter" @submit.prevent="load">
      <StationPicker v-model="query.from" />
      <StationPicker v-model="query.to" />
      <input v-model="query.date" class="input" type="date" />
      <button class="btn btn-primary" type="submit">查询</button>
    </form>

    <div class="seat-filter">
      <button type="button" :class="{ on: !seatFilter }" @click="seatFilter = ''">全部席别</button>
      <button
        v-for="seat in seatTypes"
        :key="seat.id"
        type="button"
        :class="{ on: seatFilter === seat.id }"
        @click="seatFilter = seat.id"
      >{{ seat.name }}</button>
    </div>

    <p v-if="error" class="empty">{{ error }}</p>
    <p v-else-if="loading" class="empty">正在查询余票...</p>
    <p v-else-if="!visibleTrains.length" class="empty">当天没有符合条件的车次。</p>

    <article v-for="train in visibleTrains" :key="train.trainRunId" class="card train">
      <div class="time">
        <strong>{{ train.departTime }}</strong>
        <span>{{ train.fromStationName }}</span>
      </div>
      <div class="mid">
        <b>{{ train.trainNo }}</b>
        <span>{{ train.duration }}</span>
        <small>{{ train.trainType }}</small>
      </div>
      <div class="time arrive">
        <strong>{{ train.arriveTime }}</strong>
        <span>{{ train.toStationName }}</span>
      </div>
      <div class="seats">
        <button
          v-for="seat in displaySeats(train)"
          :key="seat.seatTypeId"
          class="seat"
          :disabled="seat.available <= 0"
          type="button"
          @click="book(train, seat)"
        >
          <span>{{ seat.seatTypeName }}</span>
          <em>¥{{ formatPrice(seat.price) }}</em>
          <b>{{ ticketText(seat.available) }}</b>
        </button>
      </div>
    </article>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import StationPicker from '../components/StationPicker.vue'
import { listSeatTypes, searchTrains } from '../api/basic'

const route = useRoute()
const router = useRouter()
const trains = ref([])
const seatTypes = ref([])
const seatFilter = ref('')
const loading = ref(false)
const error = ref('')
const query = reactive({
  from: route.query.from || '北京',
  to: route.query.to || '上海',
  date: route.query.date || new Date().toISOString().slice(0, 10)
})

const visibleTrains = computed(() => {
  if (!seatFilter.value) return trains.value
  return trains.value.filter((train) =>
    (train.seats || []).some((seat) => seat.seatTypeId === seatFilter.value)
  )
})

function displaySeats(train) {
  if (!seatFilter.value) return train.seats || []
  return (train.seats || []).filter((seat) => seat.seatTypeId === seatFilter.value)
}

function formatPrice(price) {
  if (price == null || price === '') return '--'
  return Number(price).toFixed(2)
}

function ticketText(available) {
  if (available <= 0) return '无票'
  if (available > 20) return '有票'
  return `${available}张`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await searchTrains(query)
    trains.value = data.trains || []
    router.replace({ name: 'trains', query: { ...query } })
  } catch (e) {
    trains.value = []
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function book(train, seat) {
  sessionStorage.setItem('railway.selectedTrain', JSON.stringify({
    ...train,
    date: query.date,
    seatTypeId: seat.seatTypeId,
    seatTypeName: seat.seatTypeName,
    available: seat.available,
    price: seat.price
  }))
  router.push({ name: 'book' })
}

onMounted(async () => {
  try {
    seatTypes.value = await listSeatTypes()
  } catch {
    seatTypes.value = []
  }
  await load()
})

watch(() => route.query, (value) => {
  query.from = value.from || query.from
  query.to = value.to || query.to
  query.date = value.date || query.date
}, { deep: true })
</script>

<style scoped>
.filter { display: grid; grid-template-columns: 1fr 1fr 180px auto; gap: 12px; padding: 16px; margin-bottom: 12px; }
.seat-filter { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.seat-filter button {
  border: 1px solid #d7dee8;
  background: #fff;
  border-radius: 999px;
  padding: 6px 12px;
  cursor: pointer;
}
.seat-filter .on { background: #e21c21; color: #fff; border-color: #e21c21; }
.train {
  display: grid;
  grid-template-columns: 120px 1fr 120px 1.6fr;
  gap: 16px;
  padding: 20px;
  margin-bottom: 12px;
  align-items: center;
}
.time { display: flex; flex-direction: column; gap: 6px; }
.time strong { font-size: 28px; }
.arrive { text-align: right; }
.mid { text-align: center; color: #5b6573; display: flex; flex-direction: column; gap: 4px; }
.mid b { color: #1d4f86; font-size: 18px; }
.seats { display: flex; flex-wrap: wrap; gap: 8px; justify-content: flex-end; }
.seat {
  min-width: 108px;
  border: 1px solid #d7dee8;
  background: #fff;
  border-radius: 8px;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
  text-align: left;
}
.seat em { font-style: normal; color: #5b6573; font-size: 12px; }
.seat b { color: #e21c21; }
.seat:disabled { color: #9aa3af; cursor: not-allowed; }
.seat:disabled b { color: #9aa3af; }
@media (max-width: 800px) {
  .filter, .train { grid-template-columns: 1fr; }
  .arrive { text-align: left; }
}
</style>
