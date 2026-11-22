import type { OrderStatus } from '../types'
import { statusMeta } from '../utils'

export default function StatusBadge({ status, label }: { status: OrderStatus; label?: string }) {
  const meta = statusMeta[status]
  return (
    <span className={`status-badge ${meta.className}`}>
      <i style={{ background: meta.dot }} />
      {label ?? meta.label}
    </span>
  )
}
