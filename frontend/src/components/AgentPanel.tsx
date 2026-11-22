import { ArrowRight, Bot, ShieldCheck, Sparkles, Zap } from 'lucide-react'
import { Link } from 'react-router-dom'

const insights = [
  {
    icon: <Zap size={15} />,
    tone: 'lime',
    title: 'H100 利用率偏高',
    detail: '上海区域当前 78%，建议将新任务调度到北京 A100。',
  },
  {
    icon: <ShieldCheck size={15} />,
    tone: 'cyan',
    title: '2 个实例运行稳定',
    detail: '过去 24 小时无异常重启，SLA 99.99%。',
  },
  {
    icon: <Sparkles size={15} />,
    tone: 'violet',
    title: '可节省 ¥1,240 / 月',
    detail: '按当前负载，切换到 72h 预留实例更划算。',
  },
]

export default function AgentPanel() {
  return (
    <section className="agent-panel">
      <div className="agent-panel-head">
        <div className="agent-orb">
          <Bot size={19} />
        </div>
        <div>
          <span className="eyebrow">Nebula Agent</span>
          <h3>运行洞察</h3>
        </div>
        <span className="agent-live"><i /> 在线</span>
      </div>

      <div className="agent-insights">
        {insights.map((insight) => (
          <div className="agent-insight" key={insight.title}>
            <span className={`insight-icon ${insight.tone}`}>{insight.icon}</span>
            <div>
              <strong>{insight.title}</strong>
              <p>{insight.detail}</p>
            </div>
          </div>
        ))}
      </div>

      <Link to="/support" className="agent-link">
        打开售后智能体
        <ArrowRight size={15} />
      </Link>
    </section>
  )
}
