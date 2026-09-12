<template>
  <div>
    <div class="toolbar">
      <h2>票价管理</h2>
    </div>
    <form class="card form-grid" @submit.prevent="save">
      <div class="field">
        <label>车次</label>
        <select v-model="form.trainId" class="select">
          <option disabled value="">选择车次</option>
          <option v-for="train in trains" :key="train.id" :value="train.id">{{ train.trainNo }}</option>
        </select>
      </div>
      <div class="field">
        <label>席别</label>
        <select v-model="form.seatTypeId" class="select">
          <option disabled value="">选择席别</option>
          <option v-for="seat in seatTypes" :key="seat.id" :value="seat.id">{{ seat.name }}</option>
        </select>
      </div>
      <div class="field">
        <label>出发站</label>
        <select v-model="form.fromStationId" class="select">
          <option disabled value="">出发站</option>
          <option v-for="station in stations" :key="station.id" :value="station.id">{{ station.name }}</option>
        </select>
      </div>
      <div class="field">
        <label>到达站</label>
        <select v-model="form.toStationId" class="select">
          <option disabled value="">到达站</option>
          <option v-for="station in stations" :key="'to-' + station.id" :value="station.id">{{ station.name }}</option>
        </select>
      </div>
      <div class="field">
        <label>票价</label>
        <input v-model="form.price" class="input" type="number" min="0.01" step="0.01" />
      </div>
      <button class="btn btn-primary" type="submit">保存票价</button>
    </form>
    <p v-if="message" class="ok">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="card table-wrap">
      <table>
        <thead>
          <tr><th>车次</th><th>席别</th><th>区间</th><th>票价</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in fares" :key="item.id">
            <td>{{ trainName(item.trainId) }}</td>
            <td>{{ seatName(item.seatTypeId) }}</td>
            <td>{{ stationName(item.fromStationId) }} → {{ stationName(item.toStationId) }}</td>
            <td>¥{{ Number(item.price).toFixed(2) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listAdminStations, listAdminTrains, listFares, saveFare } from '../../api/admin'
import { listSeatTypes } from '../../api/basic'

const trains = ref([])
const stations = ref([])
const seatTypes = ref([])
const fares = ref([])
const error = ref('')
const message = ref('')
const form = reactive({
  trainId: '',
  seatTypeId: '',
  fromStationId: '',
  toStationId: '',
  price: ''
})

const trainName = (id) => trains.value.find((item) => item.id === id)?.trainNo || id
const seatName = (id) => seatTypes.value.find((item) => item.id === id)?.name || id
const stationName = (id) => stations.value.find((item) => item.id === id)?.name || id

async function load() {
  error.value = ''
  try {
    const [trainList, stationList, seatList, fareList] = await Promise.all([
      listAdminTrains(),
      listAdminStations(),
      listSeatTypes(),
      listFares()
    ])
    trains.value = trainList
    stations.value = stationList
    seatTypes.value = seatList
    fares.value = fareList
  } catch (e) {
    error.value = e.message
  }
}

async function save() {
  error.value = ''
  message.value = ''
  try {
    await saveFare({ ...form, price: Number(form.price) })
    message.value = '票价已保存'
    await load()
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)
</script>
