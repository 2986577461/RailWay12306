<template>
  <div>
    <div class="toolbar">
      <h2>车站管理</h2>
      <div class="ops">
        <input v-model.trim="keyword" class="input" placeholder="站名 / 城市 / 拼音 / 站码" @keyup.enter="load" />
        <button class="btn btn-ghost" type="button" @click="load">搜索</button>
        <button class="btn btn-primary" type="button" @click="open()">新增车站</button>
      </div>
    </div>
    <p v-if="error" class="empty">{{ error }}</p>
    <div class="card table-wrap">
      <table>
        <thead>
          <tr><th>ID</th><th>站码</th><th>站名</th><th>城市</th><th>拼音</th><th></th></tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.code }}</td>
            <td>{{ item.name }}</td>
            <td>{{ item.cityName }}</td>
            <td>{{ item.pinyin }}</td>
            <td class="right">
              <button class="btn-link" type="button" @click="open(item)">编辑</button>
              <button class="btn-link" type="button" @click="remove(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="editing" class="mask" @click.self="editing = false">
      <form class="card dialog" @submit.prevent="save">
        <h3>{{ form.id ? '编辑车站' : '新增车站' }}</h3>
        <div class="field"><label>站码</label><input v-model.trim="form.stationCode" class="input" placeholder="BJP" /></div>
        <div class="field"><label>站名</label><input v-model.trim="form.stationName" class="input" placeholder="北京" /></div>
        <div class="field"><label>城市</label><input v-model.trim="form.cityName" class="input" /></div>
        <div class="field"><label>拼音</label><input v-model.trim="form.pinyin" class="input" placeholder="beijing" /></div>
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
import { createStation, deleteStation, listAdminStations, updateStation } from '../../api/admin'

const items = ref([])
const keyword = ref('')
const error = ref('')
const formError = ref('')
const editing = ref(false)
const form = reactive({ id: null, stationCode: '', stationName: '', cityName: '', pinyin: '' })

async function load() {
  error.value = ''
  try {
    items.value = await listAdminStations(keyword.value)
  } catch (e) {
    error.value = e.message
  }
}

function open(item) {
  form.id = item?.id || null
  form.stationCode = item?.code || ''
  form.stationName = item?.name || ''
  form.cityName = item?.cityName || ''
  form.pinyin = item?.pinyin || ''
  formError.value = ''
  editing.value = true
}

async function save() {
  formError.value = ''
  const payload = {
    stationCode: form.stationCode,
    stationName: form.stationName,
    cityName: form.cityName,
    pinyin: form.pinyin
  }
  try {
    if (form.id) await updateStation(form.id, payload)
    else await createStation(payload)
    editing.value = false
    await load()
  } catch (e) {
    formError.value = e.message
  }
}

async function remove(item) {
  if (!confirm(`确认删除车站 ${item.name}？`)) return
  await deleteStation(item.id)
  await load()
}

onMounted(load)
</script>
