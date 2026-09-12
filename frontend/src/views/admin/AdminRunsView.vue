<template>
  <div>
    <div class="toolbar">
      <h2>开售库存</h2>
    </div>
    <section class="card steps">
      <p>前台能查到某天车次的流程：<b>建运行日期 → 自动生成席别库存并写入 Redis</b>。已开售但余票为空的车次，点一次「重灌全部库存」即可补上。票价需在「票价管理」里单独配置。</p>
      <form class="form-grid" @submit.prevent="createRun">
        <div class="field">
          <label>车次</label>
          <select v-model="form.trainId" class="select">
            <option disabled value="">选择车次</option>
            <option v-for="train in trains" :key="train.id" :value="train.id">{{ train.trainNo }}</option>
          </select>
        </div>
        <div class="field">
          <label>运行日期</label>
          <input v-model="form.runDate" class="input" type="date" />
        </div>
        <button class="btn btn-primary" type="submit">建立运行并开售</button>
        <button class="btn btn-ghost" type="button" @click="reload">重灌全部库存</button>
      </form>
      <p v-if="message" class="ok">{{ message }}</p>
      <p v-if="error" class="error">{{ error }}</p>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createTrainRun, listAdminTrains, reloadInventory } from '../../api/admin'

const trains = ref([])
const error = ref('')
const message = ref('')
const form = reactive({
  trainId: '',
  runDate: new Date().toISOString().slice(0, 10)
})

async function load() {
  trains.value = await listAdminTrains()
}

async function createRun() {
  error.value = ''
  message.value = ''
  try {
    const id = await createTrainRun({ trainId: form.trainId, runDate: form.runDate })
    message.value = `已开售，trainRunId = ${id}`
  } catch (e) {
    error.value = e.message
  }
}

async function reload() {
  error.value = ''
  message.value = ''
  try {
    await reloadInventory()
    message.value = '库存已重灌'
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)
</script>
