import { useEffect, useRef, useState } from 'react'
import {
  Bell,
  ChevronDown,
  CreditCard,
  LogOut,
  Plus,
  Search,
  Settings,
  ShieldCheck,
} from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'

const titles: Record<string, { eyebrow: string; title: string }> = {
  '/': { eyebrow: 'Workspace', title: '总览' },
  '/market': { eyebrow: 'Workspace', title: 'GPU 市场' },
  '/instances': { eyebrow: 'Workspace', title: '我的实例' },
  '/orders': { eyebrow: 'Workspace', title: '订单中心' },
  '/billing': { eyebrow: 'Finance', title: '账单与用量' },
  '/support': { eyebrow: 'Assist', title: '售后智能体' },
}

const workspaces = [
  { name: 'Production', detail: '生产环境' },
  { name: 'Staging', detail: '预发布环境' },
  { name: 'Sandbox', detail: '演示环境' },
]

interface TopbarProps {
  onRent: () => void
  onAction: (title: string, description: string) => void
}

export default function Topbar({ onRent, onAction }: TopbarProps) {
  const { pathname } = useLocation()
  const meta = titles[pathname] ?? titles['/']
  const [openMenu, setOpenMenu] = useState<'workspace' | 'user' | null>(null)
  const [workspace, setWorkspace] = useState('Production')
  const actionsRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!actionsRef.current?.contains(event.target as Node)) setOpenMenu(null)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpenMenu(null)
    }
    document.addEventListener('mousedown', closeOnOutsideClick)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('mousedown', closeOnOutsideClick)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [])

  const toggle = (menu: 'workspace' | 'user') => {
    setOpenMenu((current) => (current === menu ? null : menu))
  }

  return (
    <header className="topbar">
      <div className="page-context">
        <span>{meta.eyebrow}</span>
        <strong>{meta.title}</strong>
      </div>

      <div className="topbar-actions" ref={actionsRef}>
        <label className="search-box">
          <Search size={16} />
          <input placeholder="搜索资源、订单或实例" aria-label="搜索" />
          <kbd>⌘ K</kbd>
        </label>

        <button className="icon-button notification-button" aria-label="通知">
          <Bell size={18} />
          <span />
        </button>

        <div className="menu-anchor">
          <button
            className={`workspace-switch${openMenu === 'workspace' ? ' open' : ''}`}
            onClick={() => toggle('workspace')}
            aria-expanded={openMenu === 'workspace'}
          >
            <span className="workspace-dot" />
            {workspace}
            <ChevronDown size={14} className={openMenu === 'workspace' ? 'chevron-open' : ''} />
          </button>
          {openMenu === 'workspace' && (
            <div className="topbar-menu workspace-menu">
              <span className="menu-section-label">切换工作区</span>
              {workspaces.map((item) => (
                <button
                  key={item.name}
                  className={`workspace-option${workspace === item.name ? ' selected' : ''}`}
                  onClick={() => {
                    setWorkspace(item.name)
                    setOpenMenu(null)
                    onAction('工作区已切换', `当前工作区：${item.name} · ${item.detail}`)
                  }}
                >
                  <span className="workspace-option-dot" />
                  <span><strong>{item.name}</strong><em>{item.detail}</em></span>
                  {workspace === item.name && <span className="workspace-check">当前</span>}
                </button>
              ))}
            </div>
          )}
        </div>

        <button className="primary-button topbar-rent" onClick={onRent}>
          <Plus size={17} />
          租用 GPU
        </button>

        <div className="menu-anchor user-anchor">
          <button
            className={`topbar-user${openMenu === 'user' ? ' open' : ''}`}
            onClick={() => toggle('user')}
            aria-label="用户菜单"
            aria-expanded={openMenu === 'user'}
          >
            <span className="avatar topbar-avatar">AC</span>
            <span className="topbar-user-copy">
              <strong>Alex Chen</strong>
              <span>Administrator</span>
            </span>
            <ChevronDown size={14} className={openMenu === 'user' ? 'chevron-open' : ''} />
          </button>
          {openMenu === 'user' && (
            <div className="topbar-menu user-menu">
              <div className="user-menu-head">
                <span className="avatar topbar-avatar large">AC</span>
                <div>
                  <strong>Alex Chen</strong>
                  <span>demo@nebula.ai</span>
                </div>
                <span className="user-role">ADMIN</span>
              </div>
              <div className="menu-divider" />
              <Link to="/billing" className="menu-item" onClick={() => setOpenMenu(null)}>
                <CreditCard size={15} />
                <span><strong>账单与用量</strong><em>查看消费和发票</em></span>
              </Link>
              <Link to="/support" className="menu-item" onClick={() => setOpenMenu(null)}>
                <ShieldCheck size={15} />
                <span><strong>售后支持</strong><em>联系售后智能体</em></span>
              </Link>
              <button
                className="menu-item"
                onClick={() => {
                  setOpenMenu(null)
                  onAction('账户设置', '账户设置页面即将上线')
                }}
              >
                <Settings size={15} />
                <span><strong>账户设置</strong><em>个人资料与安全</em></span>
              </button>
              <div className="menu-divider" />
              <button
                className="menu-item danger"
                onClick={() => {
                  setOpenMenu(null)
                  onAction('目前是演示账户', '登录与退出功能会在接入认证后启用')
                }}
              >
                <LogOut size={15} />
                <span><strong>退出登录</strong><em>结束当前会话</em></span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
