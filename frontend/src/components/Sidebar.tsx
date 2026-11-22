import type { LucideIcon } from 'lucide-react'
import {
  Activity,
  Bot,
  Cpu,
  CreditCard,
  LayoutDashboard,
  LifeBuoy,
  Server,
  ShoppingBag,
  Sparkles,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'

interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  end?: boolean
  tag?: string
}

const groups: { label: string; items: NavItem[] }[] = [
  {
    label: 'Workspace',
    items: [
      { to: '/', label: '总览', icon: LayoutDashboard, end: true },
      { to: '/market', label: 'GPU 市场', icon: Cpu },
      { to: '/instances', label: '我的实例', icon: Server },
      { to: '/orders', label: '订单中心', icon: ShoppingBag },
    ],
  },
  {
    label: 'Finance',
    items: [{ to: '/billing', label: '账单与用量', icon: CreditCard }],
  },
  {
    label: 'Assist',
    items: [{ to: '/support', label: '售后智能体', icon: LifeBuoy, tag: 'AI' }],
  },
]

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark" aria-hidden="true">
          <span />
          <span />
          <span />
        </div>
        <div className="brand-copy">
          <strong>NEBULA</strong>
          <span>COMPUTE</span>
        </div>
      </div>

      <nav className="side-nav" aria-label="主导航">
        {groups.map((group) => (
          <div className="nav-group" key={group.label}>
            <p className="nav-label">{group.label}</p>
            {group.items.map((item) => {
              const Icon = item.icon
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
                >
                  <Icon size={17} strokeWidth={1.8} />
                  <span>{item.label}</span>
                  {item.tag && <em>{item.tag}</em>}
                </NavLink>
              )
            })}
          </div>
        ))}
      </nav>

      <div className="sidebar-bottom">
        <div className="cluster-card">
          <div className="cluster-card-top">
            <span className="live-dot" />
            <span>Cluster healthy</span>
            <Activity size={14} />
          </div>
          <strong>99.98%</strong>
          <p>过去 30 天可用性</p>
          <div className="tiny-bars" aria-hidden="true">
            {[38, 56, 44, 72, 61, 84, 70, 92, 76, 88, 96, 90].map((height, index) => (
              <span key={index} style={{ height: `${height}%` }} />
            ))}
          </div>
        </div>

        <div className="profile-row">
          <div className="avatar">AC</div>
          <div className="profile-copy">
            <strong>Alex Chen</strong>
            <span>Team workspace</span>
          </div>
          <Sparkles size={15} className="profile-spark" />
        </div>
      </div>
    </aside>
  )
}
