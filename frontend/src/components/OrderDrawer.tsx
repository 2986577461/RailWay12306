import { useEffect } from 'react'
import { ArrowUpRight, Check, Clock3, Copy, MapPin, Server, X } from 'lucide-react'
import type { Order } from '../types'
import { currency, orderSteps, statusMeta, stepIndexForStatus } from '../utils'
import StatusBadge from './StatusBadge'

interface OrderDrawerProps {
  order: Order | null
  onClose: () => void
  onAction: (message: string) => void
}

export default function OrderDrawer({ order, onClose, onAction }: OrderDrawerProps) {
  useEffect(() => {
    if (!order) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [order, onClose])

  if (!order) return null

  const activeStep = stepIndexForStatus(order.status)
  const terminal = order.status === 'completed' || order.status === 'refunded' || order.status === 'failed'

  return (
    <div className="drawer-backdrop" onMouseDown={(event) => event.currentTarget === event.target && onClose()}>
      <aside className="order-drawer" aria-label="订单详情">
        <div className="drawer-head">
          <div>
            <span className="eyebrow">Order detail</span>
            <h2>{order.id}</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="关闭订单详情">
            <X size={19} />
          </button>
        </div>

        <div className="drawer-status-row">
          <StatusBadge status={order.status} />
          <span className="drawer-created">创建于 {order.createdAt}</span>
        </div>

        <div className="drawer-resource">
          <div className="drawer-resource-icon"><Server size={22} /></div>
          <div>
            <span>{order.quantity} × GPU</span>
            <strong>{order.gpuName}</strong>
            <p><MapPin size={13} /> {order.region}</p>
          </div>
          <div className="drawer-price">
            <span>订单金额</span>
            <strong>{currency(order.amount)}</strong>
          </div>
        </div>

        <div className="drawer-section">
          <div className="drawer-section-title">
            <Clock3 size={16} />
            <h3>租期</h3>
          </div>
          <div className="rental-period">
            <div><span>开始</span><strong>{order.startAt}</strong></div>
            <div className="period-arrow">→</div>
            <div><span>结束</span><strong>{order.endAt}</strong></div>
            <div className="period-hours">{order.hours}h</div>
          </div>
        </div>

        <div className="drawer-section">
          <div className="drawer-section-title">
            <ArrowUpRight size={16} />
            <h3>履约进度</h3>
          </div>
          <div className="order-timeline">
            {orderSteps.map((step, index) => {
              const state = index < activeStep ? 'done' : index === activeStep ? 'current' : ''
              return (
                <div className={`timeline-step ${state}`} key={step.key}>
                  <span className="timeline-dot">{index < activeStep ? <Check size={12} /> : index + 1}</span>
                  <div>
                    <strong>{step.label}</strong>
                    <p>{index === activeStep ? '当前阶段' : index < activeStep ? '已完成' : '等待中'}</p>
                  </div>
                </div>
              )
            })}
          </div>
          {order.status === 'failed' && (
            <div className="drawer-alert">
              开通重试已耗尽，系统正在自动退款并释放配额。
            </div>
          )}
        </div>

        {order.instanceId && (
          <div className="drawer-section">
            <div className="drawer-section-title">
              <Server size={16} />
              <h3>实例</h3>
            </div>
            <div className="instance-id-row">
              <code>{order.instanceId}</code>
              <button onClick={() => onAction('实例 ID 已复制')}><Copy size={14} /></button>
            </div>
          </div>
        )}

        <div className="drawer-actions">
          {order.status === 'pending' && <button className="primary-button" onClick={() => onAction('支付流程已启动')}>去支付</button>}
          {order.status === 'running' && <button className="secondary-button" onClick={() => onAction('已打开实例控制台')}>进入实例</button>}
          {!terminal && order.status !== 'pending' && (
            <button className="secondary-button" onClick={() => onAction('售后智能体已介入诊断')}>让 Agent 诊断</button>
          )}
          {order.status === 'completed' && <button className="secondary-button" onClick={() => onAction('已复制账单')}>查看账单</button>}
          <button className="text-button" onClick={onClose}>关闭</button>
        </div>
      </aside>
    </div>
  )
}
