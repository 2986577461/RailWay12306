import { useId } from 'react'
import { usageLabels, usageSeries } from '../data'

export default function UsageChart() {
  const gradientId = useId().replace(/:/g, '')
  const width = 760
  const height = 260
  const padX = 20
  const padY = 24
  const chartW = width - padX * 2
  const chartH = height - padY * 2
  const max = 110

  const points = usageSeries.map((value, index) => {
    const x = padX + (index / (usageSeries.length - 1)) * chartW
    const y = padY + chartH - (value / max) * chartH
    return { x, y, value }
  })

  const line = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ')
  const area = `${line} L ${points[points.length - 1].x} ${height - padY} L ${points[0].x} ${height - padY} Z`

  return (
    <div className="usage-chart">
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="过去 24 小时 GPU 利用率">
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#b8f36a" stopOpacity="0.28" />
            <stop offset="70%" stopColor="#b8f36a" stopOpacity="0.04" />
            <stop offset="100%" stopColor="#b8f36a" stopOpacity="0" />
          </linearGradient>
          <filter id="chart-glow" x="-20%" y="-20%" width="140%" height="160%">
            <feGaussianBlur stdDeviation="3" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>

        {[0, 1, 2, 3].map((lineIndex) => {
          const y = padY + (lineIndex / 3) * chartH
          return (
            <g key={lineIndex}>
              <line x1={padX} y1={y} x2={width - padX} y2={y} stroke="rgba(255,255,255,.065)" strokeDasharray="3 6" />
              <text x={width - padX} y={y - 6} textAnchor="end" className="chart-axis">
                {100 - lineIndex * 33}%
              </text>
            </g>
          )
        })}

        <path d={area} fill={`url(#${gradientId})`} />
        <path d={line} fill="none" stroke="#b8f36a" strokeWidth="2.5" strokeLinecap="round" filter="url(#chart-glow)" />

        {points.map((point, index) => (
          <circle key={index} cx={point.x} cy={point.y} r={index === points.length - 3 ? 4.5 : 2.2} fill="#b8f36a" opacity={index === points.length - 3 ? 1 : 0.5} />
        ))}

        {usageLabels.map((label, index) => {
          const x = padX + (index / (usageLabels.length - 1)) * chartW
          return (
            <text key={label} x={x} y={height - 2} textAnchor="middle" className="chart-axis">
              {label}
            </text>
          )
        })}
      </svg>
    </div>
  )
}
