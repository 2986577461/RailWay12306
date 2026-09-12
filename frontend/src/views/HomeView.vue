<template>
  <section class="hero">
    <div class="page hero-inner">
      <div>
        <p class="eyebrow">余票查询</p>
        <h1>查车站、看余票、选席别</h1>
        <p class="sub">出发站和到达站支持站名、城市、拼音、站码搜索。查询结果里的 available 就是当前真实余票。</p>
      </div>
      <form class="card search-card" @submit.prevent="onSearch">
        <div class="grid">
          <div class="field">
            <label>出发地</label>
            <StationPicker v-model="form.from" placeholder="北京 / beijing / BJP" />
          </div>
          <button class="swap" type="button" @click="swap">⇄</button>
          <div class="field">
            <label>到达地</label>
            <StationPicker v-model="form.to" placeholder="上海 / shanghai / SHH" />
          </div>
          <div class="field">
            <label>出发日期</label>
            <input v-model="form.date" class="input" type="date" :min="minDate" />
          </div>
        </div>
        <button class="btn btn-primary search-btn" type="submit">查询车票</button>
      </form>
    </div>
  </section>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import StationPicker from '../components/StationPicker.vue'

const router = useRouter()
const minDate = new Date().toISOString().slice(0, 10)
const form = reactive({ from: '北京', to: '上海', date: minDate })

function swap() {
  const from = form.from
  form.from = form.to
  form.to = from
}

function onSearch() {
  if (!form.from || !form.to || !form.date) return
  router.push({ name: 'trains', query: { ...form } })
}
</script>

<style scoped>
.hero {
  background:
    radial-gradient(circle at 12% 20%, rgba(255,255,255,.18), transparent 28%),
    linear-gradient(180deg, #1d4f86 0%, #163a61 55%, #f4f6f8 55%);
  color: #fff;
  padding: 28px 0 8px;
}
.hero-inner { display: grid; gap: 24px; }
h1 { margin: 8px 0 12px; font-size: 36px; }
.eyebrow { margin: 0; letter-spacing: 2px; opacity: .8; }
.sub { max-width: 560px; opacity: .86; line-height: 1.7; }
.search-card { padding: 24px; color: #222; display: grid; gap: 18px; }
.grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr 180px;
  gap: 12px;
  align-items: end;
}
.swap {
  width: 42px;
  height: 42px;
  border: 1px solid #d7dee8;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
}
.search-btn { justify-self: start; min-width: 140px; height: 44px; }
@media (max-width: 800px) {
  .grid { grid-template-columns: 1fr; }
  h1 { font-size: 28px; }
}
</style>
