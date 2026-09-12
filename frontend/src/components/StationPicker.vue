<template>
  <div class="picker" v-click-outside="close">
    <input
      class="input"
      :value="modelValue"
      :placeholder="placeholder"
      autocomplete="off"
      @focus="open = true"
      @input="onInput"
    />
    <div v-if="open && options.length" class="menu">
      <button v-for="item in options" :key="item.id" type="button" @click="choose(item)">
        <b>{{ item.name }}</b>
        <span>{{ item.cityName }} · {{ item.code }} · {{ item.pinyin }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { listStations } from '../api/basic'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '输入站名 / 拼音 / 站码' }
})
const emit = defineEmits(['update:modelValue', 'select'])

const open = ref(false)
const options = ref([])
let timer = 0

async function search(keyword) {
  options.value = await listStations(keyword)
}

function onInput(event) {
  emit('update:modelValue', event.target.value)
  clearTimeout(timer)
  timer = window.setTimeout(() => search(event.target.value.trim()), 200)
}

function choose(item) {
  emit('update:modelValue', item.name)
  emit('select', item)
  open.value = false
}

function close() {
  open.value = false
}

const vClickOutside = {
  mounted(el, binding) {
    el.__outside = (event) => {
      if (!el.contains(event.target)) binding.value()
    }
    document.addEventListener('click', el.__outside)
  },
  unmounted(el) {
    document.removeEventListener('click', el.__outside)
  }
}

onMounted(() => search(''))
onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
.picker { position: relative; }
.menu {
  position: absolute;
  z-index: 8;
  left: 0;
  right: 0;
  top: calc(100% + 6px);
  background: #fff;
  border: 1px solid #e3e8ef;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(20, 35, 52, .12);
  max-height: 240px;
  overflow: auto;
}
.menu button {
  width: 100%;
  border: 0;
  background: #fff;
  text-align: left;
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
}
.menu button span { color: #8a93a0; font-size: 12px; }
.menu button:hover { background: #f6f8fb; }
</style>
