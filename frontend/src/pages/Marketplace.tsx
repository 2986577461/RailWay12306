import { useMemo, useState } from 'react'
import { ArrowRight, Cpu, Search, SlidersHorizontal, Sparkles, Zap } from 'lucide-react'
import GPUCard from '../components/GPUCard'
import { gpus, regionOptions } from '../data'
import type { GPU } from '../types'

interface MarketplaceProps {
  onRent: (gpu: GPU) => void
}

const architectures = ['全部', 'Hopper', 'Ampere', 'Ada Lovelace']

export default function Marketplace({ onRent }: MarketplaceProps) {
  const [architecture, setArchitecture] = useState('全部')
  const [region, setRegion] = useState(regionOptions[0])
  const [query, setQuery] = useState('')

  const filtered = useMemo(() => {
    return gpus.filter((gpu) => {
      const matchesArch = architecture === '全部' || gpu.architecture === architecture
      const matchesRegion = region === regionOptions[0] || gpu.region === region
      const matchesQuery = query.trim() === '' || gpu.name.toLowerCase().includes(query.toLowerCase())
      return matchesArch && matchesRegion && matchesQuery
    })
  }, [architecture, region, query])

  return (
    <div className="page market-page">
      <section className="page-heading">
        <div>
          <span className="eyebrow"><span className="pulse-dot" /> Live inventory</span>
          <h1>找到你的<em>下一块 GPU。</em></h1>
          <p>按小时租用，分钟级交付。所有资源均经过集群健康检查。</p>
        </div>
        <div className="heading-actions">
          <button className="secondary-button" onClick={() => onRent(gpus[0])}>
            快速下单
            <ArrowRight size={16} />
          </button>
        </div>
      </section>

      <section className="market-hero">
        <div className="hero-copy">
          <span className="hero-pill"><Sparkles size={13} /> 新到货</span>
          <h2>H100 集群<br /><em>整点放量。</em></h2>
          <p>上海区域新增 8 台 H100 SXM，适合大模型训练与长上下文推理。</p>
          <button className="primary-button" onClick={() => onRent(gpus[0])}>
            <Zap size={16} />
            锁定 H100
          </button>
        </div>
        <div className="hero-visual" aria-hidden="true">
          <div className="orbit orbit-one" />
          <div className="orbit orbit-two" />
          <div className="orbit orbit-three" />
          <div className="hero-core"><Cpu size={40} /></div>
          <div className="hero-metric metric-a"><span>可用</span><strong>07</strong></div>
          <div className="hero-metric metric-b"><span>单价</span><strong>¥42/h</strong></div>
          <div className="hero-metric metric-c"><span>交付</span><strong>~90s</strong></div>
        </div>
      </section>

      <section className="market-toolbar">
        <div className="filter-tabs">
          {architectures.map((item) => (
            <button
              key={item}
              className={architecture === item ? 'active' : ''}
              onClick={() => setArchitecture(item)}
            >
              {item}
            </button>
          ))}
        </div>
        <div className="toolbar-right">
          <label className="market-search">
            <Search size={15} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索 GPU" />
          </label>
          <div className="select-wrap market-region">
            <SlidersHorizontal size={14} />
            <select value={region} onChange={(event) => setRegion(event.target.value)}>
              {regionOptions.map((item) => <option key={item}>{item}</option>)}
            </select>
          </div>
        </div>
      </section>

      <div className="results-meta">
        <span>{filtered.length} 个资源可用</span>
        <span className="live-inventory"><i /> 库存实时更新</span>
      </div>

      <section className="gpu-grid market-grid">
        {filtered.map((gpu) => <GPUCard gpu={gpu} onRent={onRent} key={gpu.id} />)}
        {filtered.length === 0 && (
          <div className="empty-state panel">
            <Cpu size={28} />
            <h3>没有匹配的 GPU</h3>
            <p>换个架构或区域试试。</p>
            <button className="secondary-button" onClick={() => { setArchitecture('全部'); setRegion(regionOptions[0]); setQuery('') }}>
              清除筛选
            </button>
          </div>
        )}
      </section>

      <section className="market-notes">
        <div><strong>01</strong><span>分钟级交付</span><p>开通成功后实例自动进入运行态，无需等待人工审核。</p></div>
        <div><strong>02</strong><span>按秒计费</span><p>按实际使用时长结算，提前释放只收取已用部分。</p></div>
        <div><strong>03</strong><span>失败自动退款</span><p>开通重试耗尽后自动退款，并释放占用的租期配额。</p></div>
      </section>
    </div>
  )
}
