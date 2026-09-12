<template>
  <div class="page">
    <div class="head">
      <h2>乘车人</h2>
      <button class="btn btn-primary" type="button" @click="open()">新增乘车人</button>
    </div>
    <p v-if="error" class="empty">{{ error }}</p>
    <div v-else class="card list">
      <div v-if="!items.length" class="empty">还没有乘车人，先添加一位再去订票。</div>
      <div v-for="item in items" :key="item.id" class="row">
        <div>
          <b>{{ item.name }}</b>
          <span>{{ typeText(item.passengerType) }}</span>
          <p>{{ maskId(item.idCardNo) }} · {{ item.phone || '未填手机号' }}</p>
        </div>
        <div class="ops">
          <button class="btn-link" type="button" @click="open(item)">编辑</button>
          <button class="btn-link" type="button" @click="remove(item)">删除</button>
        </div>
      </div>
    </div>

    <div v-if="editing" class="mask" @click.self="editing = false">
      <form class="card dialog" @submit.prevent="save">
        <h3>{{ form.id ? '编辑乘车人' : '新增乘车人' }}</h3>
        <div class="field"><label>姓名</label><input v-model.trim="form.name" class="input" /></div>
        <div class="field"><label>身份证号</label><input v-model.trim="form.idCardNo" class="input" /></div>
        <div class="field">
          <label>类型</label>
          <select v-model.number="form.passengerType" class="select">
            <option :value="1">成人</option>
            <option :value="2">儿童</option>
            <option :value="3">学生</option>
            <option :value="4">残军</option>
          </select>
        </div>
        <div class="field"><label>手机号</label><input v-model.trim="form.phone" class="input" maxlength="11" /></div>
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
import { addPassenger, deletePassenger, listPassengers, updatePassenger } from '../api/passenger'

const items = ref([])
const error = ref('')
const formError = ref('')
const editing = ref(false)
const form = reactive({ id: null, name: '', idCardNo: '', passengerType: 1, phone: '' })

const typeText = (type) => ({ 1: '成人', 2: '儿童', 3: '学生', 4: '残军' }[type] || '成人')
const maskId = (id) => id ? id.replace(/^(.{6}).+(.{4})$/, '$1********$2') : ''

async function load() {
  try {
    items.value = await listPassengers()
  } catch (e) {
    error.value = e.message
  }
}

function open(item) {
  form.id = item?.id || null
  form.name = item?.name || ''
  form.idCardNo = item?.idCardNo || ''
  form.passengerType = item?.passengerType || 1
  form.phone = item?.phone || ''
  formError.value = ''
  editing.value = true
}

async function save() {
  formError.value = ''
  const payload = { name: form.name, idCardNo: form.idCardNo, passengerType: form.passengerType, phone: form.phone || undefined }
  try {
    if (form.id) await updatePassenger(form.id, payload)
    else await addPassenger(payload)
    editing.value = false
    await load()
  } catch (e) {
    formError.value = e.message
  }
}

async function remove(item) {
  if (!confirm(`确认删除乘车人 ${item.name}？`)) return
  await deletePassenger(item.id)
  await load()
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.list { padding: 8px 20px; }
.row { display: flex; justify-content: space-between; gap: 16px; padding: 16px 0; border-bottom: 1px solid #eef2f6; }
.row p, .row span { color: #5b6573; margin: 6px 0 0; }
.ops { display: flex; gap: 12px; align-items: center; }
.mask { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: grid; place-items: center; padding: 16px; }
.dialog { width: min(420px, 100%); padding: 24px; display: grid; gap: 12px; }
.error { margin: 0; color: #e21c21; }
</style>
