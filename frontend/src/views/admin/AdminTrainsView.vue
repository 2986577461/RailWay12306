<template>
  <div>
    <div class="toolbar">
      <h2>车次 + 经停站</h2>
      <button class="btn btn-primary" type="button" @click="open()">新增车次</button>
    </div>
    <p v-if="error" class="empty">{{ error }}</p>
    <div class="card table-wrap">
      <table>
        <thead>
          <tr><th>ID</th><th>车次</th><th>类型</th><th>始发</th><th>终到</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.trainNo }}</td>
            <td>{{ item.trainType }}</td>
            <td>{{ formatTime(item.startTime) }}</td>
            <td>{{ formatTime(item.endTime) }}</td>
            <td class="right">
              <button class="btn-link" type="button" @click="open(item)">编辑</button>
              <button class="btn-link" type="button" @click="remove(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="editing" class="mask" @click.self="editing = false">
      <form class="card dialog wide" @submit.prevent="save">
        <h3>{{ form.id ? '编辑车次' : '新增车次' }}</h3>
        <div class="grid2">
          <div class="field"><label>车次号</label><input v-model.trim="form.trainNo" class="input" placeholder="G102" /></div>
          <div class="field"><label>类型</label><input v-model.trim="form.trainType" class="input" placeholder="高速动车" /></div>
        </div>
        <div class="stops-head">
          <b>经停站（按顺序）</b>
          <button class="btn btn-ghost" type="button" @click="addStop">加一站</button>
        </div>
        <div v-for="(stop, index) in form.stops" :key="index" class="stop-row">
          <select v-model="stop.stationId" class="select">
            <option disabled value="">选择车站</option>
            <option v-for="station in stations" :key="station.id" :value="station.id">{{ station.name }} / {{ station.code }}</option>
          </select>
          <input v-model="stop.arriveTime" class="input" placeholder="到达 HH:mm" :disabled="index === 0" />
          <input v-model="stop.departTime" class="input" placeholder="出发 HH:mm" :disabled="index === form.stops.length - 1" />
          <button class="btn-link" type="button" :disabled="form.stops.length <= 2" @click="form.stops.splice(index, 1)">删</button>
        </div>
        <p v-if="formError" class="error">{{ formError }}</p>
        <div class="ops">
          <button class="btn btn-ghost" type="button" @click="editing = false">取消</button>
          <button class="btn btn-primary" type="submit">保存</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createTrain, deleteTrain, getAdminTrain, listAdminStations, listAdminTrains, updateTrain } from '../../api/admin'

const items = ref([])
const stations = ref([])
const error = ref('')
const formError = ref('')
const editing = ref(false)
const form = reactive({ id: null, trainNo: '', trainType: '高速动车', stops: [] })

function formatTime(value) {
  if (!value) return '--'
  if (Array.isArray(value)) {
    const [h = 0, m = 0] = value
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
  }
  return String(value).slice(0, 5)
}

function blankStops() {
  return [
    { stationId: '', arriveTime: '', departTime: '' },
    { stationId: '', arriveTime: '', departTime: '' }
  ]
}

async function load() {
  error.value = ''
  try {
    const [trains, stationList] = await Promise.all([listAdminTrains(), listAdminStations()])
    items.value = trains
    stations.value = stationList
  } catch (e) {
    error.value = e.message
  }
}

function addStop() {
  const last = form.stops[form.stops.length - 1]
  form.stops.splice(form.stops.length - 1, 0, { stationId: '', arriveTime: '', departTime: '' })
  if (last && !last.departTime) last.departTime = ''
}

async function open(item) {
  formError.value = ''
  if (!item) {
    form.id = null
    form.trainNo = ''
    form.trainType = '高速动车'
    form.stops = blankStops()
    editing.value = true
    return
  }
  form.id = item.id
  form.trainNo = item.trainNo || ''
  form.trainType = item.trainType || '高速动车'
  form.stops = blankStops()
  editing.value = true
  try {
    const detail = await getAdminTrain(item.id)
    form.id = detail.id
    form.trainNo = detail.trainNo
    form.trainType = detail.trainType || '高速动车'
    const stops = detail.stops || []
    form.stops = stops.length ? stops.map((stop) => ({
      stationId: stop.stationId,
      arriveTime: formatTime(stop.arriveTime) === '--' ? '' : formatTime(stop.arriveTime),
      departTime: formatTime(stop.departTime) === '--' ? '' : formatTime(stop.departTime)
    })) : blankStops()
  } catch (e) {
    formError.value = e.message || '加载车次详情失败'
  }
}

function payload() {
  return {
    trainNo: form.trainNo,
    trainType: form.trainType,
    stops: form.stops.map((stop, index) => ({
      stationId: stop.stationId,
      stopSeq: index + 1,
      arriveTime: index === 0 ? null : (stop.arriveTime || null),
      departTime: index === form.stops.length - 1 ? null : (stop.departTime || null)
    }))
  }
}

async function save() {
  formError.value = ''
  try {
    if (form.id) await updateTrain(form.id, payload())
    else await createTrain(payload())
    editing.value = false
    await load()
  } catch (e) {
    formError.value = e.message
  }
}

async function remove(item) {
  if (!confirm(`确认删除车次 ${item.trainNo}？`)) return
  await deleteTrain(item.id)
  await load()
}

onMounted(load)
</script>
