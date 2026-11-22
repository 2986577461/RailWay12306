import { ArrowRight, Copy, Cpu, ExternalLink, MapPin, RefreshCw, Server, ShieldCheck, Sparkles, Terminal } from 'lucide-react'
import { Link } from 'react-router-dom'
import { instances } from '../data'
import { currency } from '../utils'

interface InstancesProps {
  onAction: (message: string) => void
}

const statusLabel = {
  running: '运行中',
  provisioning: '开通中',
  degraded: '需关注',
}

export default function Instances({ onAction }: InstancesProps) {
  return (
    <div className="page instances-page">
      <section className="page-heading">
        <div>
          <span className="eyebrow"><span className="pulse-dot" /> Infrastructure</span>
          <h1>我的<em>实例。</em></h1>
          <p>查看 GPU 运行状态、连接信息与实时利用率。</p>
        </div>
        <div className="heading-actions">
          <button className="secondary-button" onClick={() => onAction('实例状态已刷新')}>
            <RefreshCw size={16} />
            刷新状态
          </button>
        </div>
      </section>

      <section className="agent-scan-banner">
        <div className="scan-orb"><Sparkles size={19} /></div>
        <div>
          <strong>售后智能体已完成例行巡检</strong>
          <p>1 个实例利用率偏低，1 个实例正在开通。没有发现阻断性异常。</p>
        </div>
        <Link to="/support" className="secondary-button">查看诊断 <ArrowRight size={15} /></Link>
      </section>

      <section className="instance-grid">
        {instances.map((instance) => (
          <article className={`instance-card ${instance.status}`} key={instance.id}>
            <div className="instance-card-head">
              <span className={`instance-state ${instance.status}`}><i /> {statusLabel[instance.status]}</span>
              <span className="instance-region"><MapPin size={13} /> {instance.region}</span>
            </div>

            <div className="instance-title-row">
              <div className="instance-gpu-mark"><Cpu size={22} /></div>
              <div>
                <h3>{instance.gpuName}</h3>
                <p>{instance.id}</p>
              </div>
              <div className="instance-price">
                <strong>{currency(instance.pricePerHour)}</strong>
                <span>/ 小时</span>
              </div>
            </div>

            <div className="instance-util-block">
              <div>
                <span>GPU 利用率</span>
                <strong>{instance.utilization}%</strong>
              </div>
              <div className="instance-util-bar"><span style={{ width: `${instance.utilization}%` }} /></div>
              <div className="instance-util-meta">
                <span>运行时长 {instance.uptime}</span>
                <span>订单 {instance.orderId}</span>
              </div>
            </div>

            <div className="terminal-preview">
              <div className="terminal-head"><Terminal size={14} /> quick connect <span>bash</span></div>
              <code>$ nebula ssh {instance.id}</code>
              <code className="terminal-muted">{instance.status === 'provisioning' ? 'waiting for allocation...' : `connected · ${instance.ip}`}</code>
            </div>

            <div className="endpoint-row">
              <span>{instance.endpoint}</span>
              <button onClick={() => onAction('Endpoint 已复制')} aria-label="复制 endpoint"><Copy size={14} /></button>
            </div>

            <div className="instance-actions">
              <button className="primary-button" onClick={() => onAction(instance.status === 'provisioning' ? '实例仍在开通中' : '已打开 Web 终端')} disabled={instance.status === 'provisioning'}>
                <ExternalLink size={15} />
                连接实例
              </button>
              <Link to="/support" className="secondary-button">
                <ShieldCheck size={15} />
                诊断
              </Link>
            </div>
          </article>
        ))}

        <article className="instance-card add-instance-card">
          <span className="add-instance-icon"><Server size={24} /></span>
          <h3>还需要更多算力？</h3>
          <p>从市场选择新的 GPU 规格，分钟级交付。</p>
          <Link to="/market" className="primary-button">前往 GPU 市场 <ArrowRight size={15} /></Link>
        </article>
      </section>
    </div>
  )
}
