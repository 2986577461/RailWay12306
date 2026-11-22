import { useEffect, useMemo, useState } from 'react'
import { Clock, MapPin, Minus, Plus, ShieldCheck, Sparkles, X } from 'lucide-react'
import type { GPU, Order } from '../types'
import { currency } from '../utils'

interface RentModalProps {
  gpu: GPU | null
  onClose: () => void
  onCreate: (order: Order) => void
}

const presets = [1, 8, 24, 72]

const formatDate = (date: Date) => {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export default function RentModal({ gpu, onClose, onCreate }: RentModalProps) {
  const [hours, setHours] = useState(8)
  const [quantity, setQuantity] = useState(1)
  const [region, setRegion] = useState('')

  useEffect(() => {
    if (!gpu) return
    setHours(8)
    setQuantity(1)
    setRegion(gpu.region)
  }, [gpu])

  useEffect(() => {
    if (!gpu) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [gpu, onClose])

  const total = useMemo(() => (gpu ? gpu.pricePerHour * hours * quantity : 0), [gpu, hours, quantity])

  if (!gpu) return null

  const start = new Date()
  const end = new Date(start.getTime() + hours * 60 * 60 * 1000)

  const submit = () => {
    const order: Order = {
      id: `ORD-${Date.now().toString().slice(-8)}`,
      gpuId: gpu.id,
      gpuName: gpu.name,
      region,
      quantity,
      startAt: formatDate(start),
      endAt: formatDate(end),
      hours,
      amount: total,
      status: 'pending',
      createdAt: formatDate(new Date()),
    }
    onCreate(order)
  }

  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.currentTarget === event.target && onClose()}>
      <section className="rent-modal" role="dialog" aria-modal="true" aria-labelledby="rent-title">
        <div className="modal-head">
          <div>
            <span className="eyebrow">Reserve compute</span>
            <h2 id="rent-title">配置租用</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="关闭">
            <X size={19} />
          </button>
        </div>

        <div className="modal-body">
          <div className="modal-main">
            <div className="selected-gpu">
              <div className="selected-gpu-orb" style={{ background: gpu.accent }} />
              <div>
                <span>{gpu.architecture} · {gpu.memoryType}</span>
                <strong>{gpu.name}</strong>
                <p>{gpu.memory} 显存 · {gpu.tflops} 算力</p>
              </div>
              <span className="selected-available">{gpu.available} 台可用</span>
            </div>

            <div className="form-block">
              <label className="form-label">租用时长</label>
              <div className="preset-row">
                {presets.map((preset) => (
                  <button
                    key={preset}
                    className={`preset${hours === preset ? ' active' : ''}`}
                    onClick={() => setHours(preset)}
                  >
                    {preset}h
                  </button>
                ))}
              </div>
              <div className="range-row">
                <input
                  type="range"
                  min="1"
                  max="72"
                  value={hours}
                  onChange={(event) => setHours(Number(event.target.value))}
                  aria-label="租用时长"
                />
                <span><Clock size={14} /> {hours} 小时</span>
              </div>
            </div>

            <div className="form-grid">
              <div className="form-block">
                <label className="form-label">GPU 数量</label>
                <div className="stepper">
                  <button onClick={() => setQuantity((value) => Math.max(1, value - 1))} disabled={quantity <= 1}>
                    <Minus size={15} />
                  </button>
                  <strong>{quantity}</strong>
                  <button onClick={() => setQuantity((value) => Math.min(gpu.available, value + 1))} disabled={quantity >= gpu.available}>
                    <Plus size={15} />
                  </button>
                </div>
              </div>

              <div className="form-block">
                <label className="form-label">区域</label>
                <div className="select-wrap">
                  <MapPin size={15} />
                  <select value={region} onChange={(event) => setRegion(event.target.value)}>
                    <option>{gpu.region}</option>
                    <option>北京 · CN-North</option>
                    <option>深圳 · CN-South</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="rent-note">
              <ShieldCheck size={16} />
              <span>开通失败自动重试，重试耗尽将全额退款并释放配额。</span>
            </div>
          </div>

          <aside className="modal-summary">
            <span className="eyebrow">Order summary</span>
            <h3>{gpu.name}</h3>
            <div className="summary-lines">
              <div><span>单价</span><strong>{currency(gpu.pricePerHour)} / 小时</strong></div>
              <div><span>数量</span><strong>{quantity} GPU</strong></div>
              <div><span>时长</span><strong>{hours} 小时</strong></div>
              <div><span>区域</span><strong>{region.split(' · ')[0]}</strong></div>
            </div>
            <div className="summary-total">
              <span>应付金额</span>
              <strong>{currency(total)}</strong>
              <small>含税 · 按秒计费</small>
            </div>
            <button className="primary-button modal-submit" onClick={submit}>
              <Sparkles size={16} />
              创建订单
            </button>
            <button className="text-button" onClick={onClose}>稍后再说</button>
          </aside>
        </div>
      </section>
    </div>
  )
}
