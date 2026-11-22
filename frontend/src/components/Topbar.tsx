import { Bell, ChevronDown, Plus, Search } from 'lucide-react'
import { useLocation } from 'react-router-dom'

const titles: Record<string, { eyebrow: string; title: string }> = {
  '/': { eyebrow: 'Workspace', title: '总览' },
  '/market': { eyebrow: 'Workspace', title: 'GPU 市场' },
  '/instances': { eyebrow: 'Workspace', title: '我的实例' },
  '/orders': { eyebrow: 'Workspace', title: '订单中心' },
  '/billing': { eyebrow: 'Finance', title: '账单与用量' },
  '/support': { eyebrow: 'Assist', title: '售后智能体' },
}

interface TopbarProps {
  onRent: () => void
}

export default function Topbar({ onRent }: TopbarProps) {
  const { pathname } = useLocation()
  const meta = titles[pathname] ?? titles['/']

  return (
    <header className="topbar">
      <div className="page-context">
        <span>{meta.eyebrow}</span>
        <strong>{meta.title}</strong>
      </div>

      <div className="topbar-actions">
        <label className="search-box">
          <Search size={16} />
          <input placeholder="搜索资源、订单或实例" aria-label="搜索" />
          <kbd>⌘ K</kbd>
        </label>

        <button className="icon-button notification-button" aria-label="通知">
          <Bell size={18} />
          <span />
        </button>

        <button className="workspace-switch">
          <span className="workspace-dot" />
          Production
          <ChevronDown size={14} />
        </button>

        <button className="primary-button topbar-rent" onClick={onRent}>
          <Plus size={17} />
          租用 GPU
        </button>
      </div>
    </header>
  )
}
