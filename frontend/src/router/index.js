import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
    { path: '/trains', name: 'trains', component: () => import('../views/TrainsView.vue') },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
    { path: '/book', name: 'book', component: () => import('../views/BookView.vue'), meta: { auth: true } },
    { path: '/passengers', name: 'passengers', component: () => import('../views/PassengersView.vue'), meta: { auth: true } },
    { path: '/orders', name: 'orders', component: () => import('../views/OrdersView.vue'), meta: { auth: true } },
    { path: '/orders/:orderNo', name: 'order-detail', component: () => import('../views/OrderDetailView.vue'), meta: { auth: true } },
    { path: '/profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { auth: true } },
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      children: [
        { path: '', redirect: '/admin/stations' },
        { path: 'stations', name: 'admin-stations', component: () => import('../views/admin/AdminStationsView.vue') },
        { path: 'trains', name: 'admin-trains', component: () => import('../views/admin/AdminTrainsView.vue') },
        { path: 'fares', name: 'admin-fares', component: () => import('../views/admin/AdminFaresView.vue') },
        { path: 'runs', name: 'admin-runs', component: () => import('../views/admin/AdminRunsView.vue') }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
