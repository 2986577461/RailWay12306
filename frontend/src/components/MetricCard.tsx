import type { ReactNode } from 'react'
import { ArrowDownRight, ArrowUpRight } from 'lucide-react'

interface MetricCardProps {
  label: string
  value: string
  delta: string
  direction?: 'up' | 'down' | 'flat'
  icon: ReactNode
  detail: string
  tone?: 'green' | 'cyan' | 'violet' | 'amber'
}

export default function MetricCard({
  label,
  value,
  delta,
  direction = 'up',
  icon,
  detail,
  tone = 'green',
}: MetricCardProps) {
  return (
    <article className={`metric-card tone-${tone}`}>
      <div className="metric-top">
        <span className="metric-icon">{icon}</span>
        <span className={`metric-delta ${direction}`}>
          {direction === 'down' ? <ArrowDownRight size={13} /> : <ArrowUpRight size={13} />}
          {delta}
        </span>
      </div>
      <p>{label}</p>
      <strong>{value}</strong>
      <span className="metric-detail">{detail}</span>
    </article>
  )
}
