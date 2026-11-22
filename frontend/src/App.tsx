import { useCallback, useRef, useState } from 'react'
import { Route, Routes } from 'react-router-dom'
import RentModal from './components/RentModal'
import Sidebar from './components/Sidebar'
import Toast from './components/Toast'
import Topbar from './components/Topbar'
import { gpus, seedOrders } from './data'
import Billing from './pages/Billing'
import Dashboard from './pages/Dashboard'
import Instances from './pages/Instances'
import Marketplace from './pages/Marketplace'
import Orders from './pages/Orders'
import Support from './pages/Support'
import type { GPU, Order, ToastMessage } from './types'

export default function App() {
  const [orders, setOrders] = useState<Order[]>(seedOrders)
  const [rentTarget, setRentTarget] = useState<GPU | null>(null)
  const [toast, setToast] = useState<ToastMessage | null>(null)
  const toastTimer = useRef<number | undefined>(undefined)

  const showToast = useCallback((title: string, description: string, tone: ToastMessage['tone'] = 'success') => {
    window.clearTimeout(toastTimer.current)
    setToast({ id: Date.now(), title, description, tone })
    toastTimer.current = window.setTimeout(() => setToast(null), 4200)
  }, [])

  const createOrder = (order: Order) => {
    setOrders((current) => [order, ...current])
    setRentTarget(null)
    showToast('订单已创建', `${order.id} 已进入待支付状态，配额已为你保留 15 分钟。`)
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <div className="app-main">
        <Topbar onRent={() => setRentTarget(gpus[0])} />
        <main className="app-content">
          <Routes>
            <Route path="/" element={<Dashboard onRent={setRentTarget} orders={orders} />} />
            <Route path="/market" element={<Marketplace onRent={setRentTarget} />} />
            <Route path="/orders" element={<Orders orders={orders} onAction={(message) => showToast('操作已执行', message, 'info')} />} />
            <Route path="/instances" element={<Instances onAction={(message) => showToast('操作已执行', message, 'info')} />} />
            <Route path="/billing" element={<Billing />} />
            <Route path="/support" element={<Support orders={orders} />} />
          </Routes>
        </main>
      </div>

      <RentModal gpu={rentTarget} onClose={() => setRentTarget(null)} onCreate={createOrder} />
      <Toast toast={toast} onClose={() => setToast(null)} />
    </div>
  )
}
