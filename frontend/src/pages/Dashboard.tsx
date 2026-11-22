import type { CSSProperties } from 'react'
import {
  Activity,
  ArrowRight,
  CircleDollarSign,
  Cpu,
  Server,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  Zap,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import AgentPanel from '../components/AgentPanel'
import GPUCard from '../components/GPUCard'
import MetricCard from '../components/MetricCard'
import StatusBadge from '../components/StatusBadge'
import UsageChart from '../components/UsageChart'
import { gpus, instances } from '../data'
import type { GPU, Order } from '../types'
import { currency, number } from '../utils'

interface DashboardProps {
  onRent: (gpu: GPU) => void
  orders: Order[]
}

export default function Dashboard({ onRent, orders }: DashboardProps) {
  return (
    <div className="page dashboard-page">
      <section className="page-heading dashboard-heading">
        <div>
          <span className="eyebrow"><span className="pulse-dot" /> Sunday, 22 Nov · UTC+8</span>
          <h1>算力，<em>随时就绪。</em></h1>
          <p>下午好，Alex。你的集群运行平稳，当前有 2 个实例承载生产任务。</p>
        </div>
        <div className="heading-actions">
          <Link className="secondary-button" to="/instances">
            查看实例
            <ArrowRight size={16} />
          </Link>
          <button className="primary-button" onClick={() => onRent(gpus[0])}>
            <Zap size={16} />
            快速租用
          </button>
        </div>
      </section>

      <section className="metric-grid">
        <MetricCard
          label="运行中实例"
          value="02"
          delta="+1 本周"
          icon={<Server size={18} />}
          detail="共 3 个 GPU 资源"
          tone="green"
        />
        <MetricCard
          label="本月支出"
          value="¥8,420"
          delta="+12.4%"
          icon={<CircleDollarSign size={18} />}
          detail="预算使用 68%"
          tone="cyan"
        />
        <MetricCard
          label="平均利用率"
          value="78.4%"
          delta="+6.2%"
          icon={<Activity size={18} />}
          detail="较上周更高效"
          tone="violet"
        />
        <MetricCard
          label="履约成功率"
          value="99.98%"
          delta="SLA"
          direction="flat"
          icon={<ShieldCheck size={18} />}
          detail="过去 30 天"
          tone="amber"
        />
      </section>

      <section className="dashboard-grid">
        <article className="panel usage-panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">Compute pulse</span>
              <h2>集群利用率</h2>
            </div>
            <div className="segmented-control" aria-label="时间范围">
              <button className="active">24h</button>
              <button>7d</button>
              <button>30d</button>
            </div>
          </div>
          <div className="chart-summary">
            <div>
              <strong>78.4%</strong>
              <span><TrendingUp size={14} /> 当前平均利用率</span>
            </div>
            <div className="chart-legend">
              <span><i className="legend-lime" /> GPU 利用率</span>
              <span><i className="legend-muted" /> 基线</span>
            </div>
          </div>
          <UsageChart />
          <div className="chart-footer">
            <div><span>峰值</span><strong>98%</strong></div>
            <div><span>任务数</span><strong>{number(1842)}</strong></div>
            <div><span>节省估算</span><strong>{currency(1240)}</strong></div>
            <Link to="/billing">查看用量明细 <ArrowRight size={14} /></Link>
          </div>
        </article>

        <AgentPanel />
      </section>

      <section className="dashboard-lower">
        <article className="panel active-panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">Live now</span>
              <h2>活跃实例</h2>
            </div>
            <Link className="panel-link" to="/instances">全部实例 <ArrowRight size={14} /></Link>
          </div>
          <div className="instance-mini-list">
            {instances.map((instance) => (
              <div className="instance-mini" key={instance.id}>
                <div className={`instance-status-orb ${instance.status}`}>
                  <Server size={16} />
                </div>
                <div className="instance-mini-main">
                  <strong>{instance.gpuName}</strong>
                  <span>{instance.id} · {instance.region.split(' · ')[0]}</span>
                </div>
                <div className="instance-mini-util">
                  <div className="util-ring" style={{ '--value': `${instance.utilization * 3.6}deg` } as CSSProperties}>
                    <span>{instance.utilization}%</span>
                  </div>
                </div>
                <div className="instance-mini-time">
                  <span>运行时长</span>
                  <strong>{instance.uptime}</strong>
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="panel recent-panel">
          <div className="panel-head">
            <div>
              <span className="eyebrow">Recent activity</span>
              <h2>最近订单</h2>
            </div>
            <Link className="panel-link" to="/orders">订单中心 <ArrowRight size={14} /></Link>
          </div>
          <div className="recent-table">
            <div className="recent-table-head">
              <span>订单</span><span>资源</span><span>状态</span><span>金额</span>
            </div>
            {orders.slice(0, 3).map((order) => (
              <Link to="/orders" className="recent-row" key={order.id}>
                <span><strong>{order.id}</strong><em>{order.createdAt.slice(5, 16)}</em></span>
                <span><Cpu size={14} /> {order.gpuName.replace('NVIDIA ', '')} × {order.quantity}</span>
                <StatusBadge status={order.status} />
                <span className="recent-amount">{currency(order.amount)}</span>
              </Link>
            ))}
          </div>
        </article>
      </section>

      <section className="market-strip">
        <div className="strip-heading">
          <div>
            <span className="eyebrow"><Sparkles size={13} /> Curated for you</span>
            <h2>热门算力</h2>
          </div>
          <Link className="panel-link" to="/market">进入市场 <ArrowRight size={14} /></Link>
        </div>
        <div className="gpu-grid compact-grid">
          {gpus.slice(0, 3).map((gpu) => (
            <GPUCard gpu={gpu} onRent={onRent} compact key={gpu.id} />
          ))}
        </div>
      </section>
    </div>
  )
}
