export type OrderStatus =
  | 'pending'
  | 'paid'
  | 'provisioning'
  | 'running'
  | 'completed'
  | 'refunding'
  | 'refunded'
  | 'failed'

export interface GPU {
  id: string
  name: string
  architecture: string
  memory: string
  memoryType: string
  bandwidth: string
  tflops: string
  pricePerHour: number
  available: number
  total: number
  region: string
  badge?: string
  accent: string
  description: string
  useCases: string[]
}

export interface Order {
  id: string
  gpuId: string
  gpuName: string
  region: string
  quantity: number
  startAt: string
  endAt: string
  hours: number
  amount: number
  status: OrderStatus
  createdAt: string
  instanceId?: string
}

export interface Instance {
  id: string
  orderId: string
  gpuName: string
  region: string
  status: 'running' | 'provisioning' | 'degraded'
  ip: string
  uptime: string
  utilization: number
  pricePerHour: number
  endpoint: string
}

export interface ToastMessage {
  id: number
  title: string
  description: string
  tone: 'success' | 'info' | 'warning'
}
