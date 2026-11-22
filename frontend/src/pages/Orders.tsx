import { useMemo, useState } from 'react'
import { ArrowDownToLine, ChevronRight, CircleDollarSign, Clock3, Cpu, ListFilter, Server, ShoppingBag } from 'lucide-react'
import OrderDrawer from '../components/OrderDrawer'
import StatusBadge from '../components/StatusBadge'
import type { Order, OrderStatus } from '../types'
import { currency } from '../utils'

interface OrdersProps {
  orders: Order[]
  onAction: (message: string) => void
}

type FilterKey = 'all' | 'pending' | 'provisioning' | 'running' | 'completed'

const filters: { key: FilterKey; label: string; statuses?: OrderStatus[] }[] = [
  { key: 'all', label: '全部订单' },
  { key: 'pending', label: '待支付', statuses: ['pending'] },
  { key: 'provisioning', label: '开通中', statuses: ['paid', 'provisioning', 'refunding'] },
  { key: 'running', label: '运行中', statuses: ['running'] },
  { key: 'completed', label: '已完成', statuses: ['completed', 'refunded', 'failed'] },
]

export default function Orders({ orders, onAction }: OrdersProps) {
  const [filter, setFilter] = useState<FilterKey>('all')
  const [selected, setSelected] = useState<Order | null>(null)

  const filtered = useMemo(() => {
    const current = filters.find((item) => item.key === filter)
    if (!current?.statuses) return orders
    return orders.filter((order) => current.statuses?.includes(order.status))
  }, [filter, orders])

  const runningCount = orders.filter((order) => order.status === 'running').length
  const pendingCount = orders.filter((order) => order.status === 'pending').length
  const totalSpend = orders.filter((order) => order.status !== 'pending' && order.status !== 'refunded').reduce((sum, order) => sum + order.amount, 0)

  return (
    <div className="page orders-page">
      <section className="page-heading">
        <div>
          <span className="eyebrow"><ListFilter size={13} /> Order lifecycle</span>
          <h1>订单<em>中心。</em></h1>
          <p>从支付、开通到释放，一条订单链路完整可追踪。</p>
        </div>
        <div className="heading-actions">
          <button className="secondary-button" onClick={() => onAction('订单明细已导出')}>
            <ArrowDownToLine size={16} />
            导出明细
          </button>
        </div>
      </section>

      <section className="order-summary-grid">
        <div className="order-summary-card">
          <span className="summary-icon green"><ShoppingBag size={17} /></span>
          <div><span>全部订单</span><strong>{orders.length}</strong></div>
          <em>本月</em>
        </div>
        <div className="order-summary-card">
          <span className="summary-icon cyan"><Server size={17} /></span>
          <div><span>运行中</span><strong>{runningCount}</strong></div>
          <em>Live</em>
        </div>
        <div className="order-summary-card">
          <span className="summary-icon amber"><Clock3 size={17} /></span>
          <div><span>待支付</span><strong>{pendingCount}</strong></div>
          <em>需处理</em>
        </div>
        <div className="order-summary-card">
          <span className="summary-icon violet"><CircleDollarSign size={17} /></span>
          <div><span>累计消费</span><strong>{currency(totalSpend)}</strong></div>
          <em>含历史</em>
        </div>
      </section>

      <section className="panel orders-panel">
        <div className="orders-toolbar">
          <div className="filter-tabs order-tabs">
            {filters.map((item) => (
              <button key={item.key} className={filter === item.key ? 'active' : ''} onClick={() => setFilter(item.key)}>
                {item.label}
                {item.key === 'provisioning' && <span>2</span>}
              </button>
            ))}
          </div>
          <span className="orders-count">{filtered.length} 条记录</span>
        </div>

        <div className="orders-table">
          <div className="orders-table-head">
            <span>订单号</span>
            <span>资源</span>
            <span>租期</span>
            <span>金额</span>
            <span>状态</span>
            <span />
          </div>
          {filtered.map((order) => (
            <button className="orders-row" key={order.id} onClick={() => setSelected(order)}>
              <span className="order-id-cell">
                <strong>{order.id}</strong>
                <em>{order.createdAt}</em>
              </span>
              <span className="order-resource-cell">
                <span className="order-gpu-icon"><Cpu size={15} /></span>
                <span><strong>{order.gpuName.replace('NVIDIA ', '')}</strong><em>{order.quantity} GPU · {order.region.split(' · ')[0]}</em></span>
              </span>
              <span className="order-period-cell">
                <strong>{order.startAt.slice(5, 16)}</strong>
                <em>{order.hours} 小时</em>
              </span>
              <span className="order-amount-cell">{currency(order.amount)}</span>
              <span><StatusBadge status={order.status} /></span>
              <span className="row-arrow"><ChevronRight size={16} /></span>
            </button>
          ))}
          {filtered.length === 0 && (
            <div className="empty-state orders-empty">
              <ShoppingBag size={26} />
              <h3>这个状态下没有订单</h3>
              <p>切换筛选条件看看。</p>
            </div>
          )}
        </div>
      </section>

      <OrderDrawer order={selected} onClose={() => setSelected(null)} onAction={onAction} />
    </div>
  )
}
