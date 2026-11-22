import type { CSSProperties } from 'react'
import { ArrowUpRight, Cpu, MapPin } from 'lucide-react'
import type { GPU } from '../types'
import { currency } from '../utils'

interface GPUCardProps {
  gpu: GPU
  onRent: (gpu: GPU) => void
  compact?: boolean
}

export default function GPUCard({ gpu, onRent, compact = false }: GPUCardProps) {
  const availability = Math.round((gpu.available / gpu.total) * 100)
  const style = { '--accent': gpu.accent } as CSSProperties

  return (
    <article className={`gpu-card${compact ? ' compact' : ''}`} style={style}>
      <div className="gpu-card-glow" />
      <div className="gpu-card-head">
        <span className="gpu-icon">
          <Cpu size={19} />
        </span>
        <div className="gpu-tags">
          {gpu.badge && <span className="gpu-badge">{gpu.badge}</span>}
          <span className="gpu-arch">{gpu.architecture}</span>
        </div>
      </div>

      <h3>{gpu.name}</h3>
      {!compact && <p className="gpu-description">{gpu.description}</p>}

      <div className="gpu-specs">
        <div>
          <span>显存</span>
          <strong>{gpu.memory}</strong>
          <em>{gpu.memoryType}</em>
        </div>
        <div>
          <span>带宽</span>
          <strong>{gpu.bandwidth}</strong>
          <em>Memory</em>
        </div>
        <div>
          <span>算力</span>
          <strong>{gpu.tflops}</strong>
          <em>FP16 Tensor</em>
        </div>
      </div>

      <div className="availability">
        <div className="availability-label">
          <span>实时可用</span>
          <strong>{gpu.available} / {gpu.total}</strong>
        </div>
        <div className="availability-bar">
          <span style={{ width: `${availability}%` }} />
        </div>
      </div>

      <div className="gpu-card-bottom">
        <div className="gpu-price">
          <strong>{currency(gpu.pricePerHour)}</strong>
          <span>/ GPU / 小时</span>
        </div>
        <button className="gpu-rent" onClick={() => onRent(gpu)}>
          立即租用
          <ArrowUpRight size={16} />
        </button>
      </div>

      <div className="gpu-region">
        <MapPin size={13} />
        {gpu.region}
      </div>
    </article>
  )
}
