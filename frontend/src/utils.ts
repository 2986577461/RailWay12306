import type { OrderStatus } from './types'

export const currency = (value: number) =>
  new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: value % 1 === 0 ? 0 : 2,
  }).format(value)

export const number = (value: number) =>
  new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }).format(value)

export const statusMeta: Record<
  OrderStatus,
  { label: string; className: string; dot: string }
> = {
  pending: { label: '待支付', className: 'status-amber', dot: '#f5c86b' },
  paid: { label: '已支付', className: 'status-cyan', dot: '#65d9f7' },
  provisioning: { label: '开通中', className: 'status-cyan', dot: '#65d9f7' },
  running: { label: '运行中', className: 'status-green', dot: '#b8f36a' },
  completed: { label: '已完成', className: 'status-muted', dot: '#8b94a7' },
  refunding: { label: '退款中', className: 'status-amber', dot: '#f5c86b' },
  refunded: { label: '已退款', className: 'status-muted', dot: '#8b94a7' },
  failed: { label: '开通失败', className: 'status-red', dot: '#ff7c7c' },
}

export const orderSteps: { key: string; label: string }[] = [
  { key: 'pending', label: '创建订单' },
  { key: 'paid', label: '支付完成' },
  { key: 'provisioning', label: '资源开通' },
  { key: 'running', label: '稳定运行' },
  { key: 'completed', label: '到期释放' },
]

export const stepIndexForStatus = (status: OrderStatus) => {
  if (status === 'pending') return 0
  if (status === 'paid') return 1
  if (status === 'provisioning') return 2
  if (status === 'running' || status === 'refunding' || status === 'failed') return 3
  if (status === 'completed' || status === 'refunded') return 4
  return 0
}
