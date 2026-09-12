<template>
  <div class="app-shell" :class="{ admin: isAdmin }">
    <AppHeader v-if="!isAdmin" />
    <main class="app-main">
      <RouterView />
    </main>
    <footer v-if="!isAdmin" class="app-footer">铁路客票演示系统 · 支付为 mock 验签回调</footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'

const route = useRoute()
const isAdmin = computed(() => route.path.startsWith('/admin'))
</script>
