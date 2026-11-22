import { FormEvent, useEffect, useRef, useState } from 'react'
import { Bot, CheckCircle2, FileText, Send, ShieldCheck, Sparkles, Terminal, Wrench } from 'lucide-react'
import type { Order } from '../types'

interface SupportProps {
  orders: Order[]
}

interface ChatMessage {
  id: number
  role: 'agent' | 'user'
  content: string
  meta?: string
}

const suggestions = [
  '为什么实例一直显示开通中？',
  '帮我查看当前订单的履约状态',
  '提前退租会怎么退款？',
]

const initialMessages: ChatMessage[] = [
  {
    id: 1,
    role: 'agent',
    content: '你好，我是 Nebula 售后智能体。我已经加载了你的订单、实例和最近 24 小时的运行日志。需要我帮你排查什么？',
    meta: '已连接订单系统 · 实例监控 · 售后政策库',
  },
  {
    id: 2,
    role: 'user',
    content: 'ORD-2408-1019 怎么还在开通中？',
  },
  {
    id: 3,
    role: 'agent',
    content: '我查到这笔订单已支付成功，开通任务在 14:19 提交。当前 provider 返回的是配额排队，不是失败。系统会在 14:24 自动重试，预计还需 2～4 分钟。需要我在开通失败时自动发起退款吗？',
    meta: '调用工具：get_order · get_provision_logs · get_policy',
  },
]

export default function Support({ orders }: SupportProps) {
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages)
  const [input, setInput] = useState('')
  const [typing, setTyping] = useState(false)
  const chatEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, typing])

  const send = (event?: FormEvent) => {
    event?.preventDefault()
    const content = input.trim()
    if (!content || typing) return
    const userMessage: ChatMessage = { id: Date.now(), role: 'user', content }
    setMessages((current) => [...current, userMessage])
    setInput('')
    setTyping(true)

    window.setTimeout(() => {
      setMessages((current) => [
        ...current,
        {
          id: Date.now() + 1,
          role: 'agent',
          content: '我看了一下当前上下文：你的订单支付和配额锁定都正常，问题集中在 provider 开通队列。建议再等待一个重试周期；如果 5 分钟后仍未完成，我可以直接帮你创建退款或升级工单。',
          meta: '调用工具：get_order · get_instance · create_ticket（需确认）',
        },
      ])
      setTyping(false)
    }, 900)
  }

  const currentOrder = orders[0]

  return (
    <div className="page support-page">
      <section className="page-heading">
        <div>
          <span className="eyebrow"><Sparkles size={13} /> After-sales intelligence</span>
          <h1>售后<em>智能体。</em></h1>
          <p>它不只回答问题，还能查订单、读日志、发起退款和升级工单。</p>
        </div>
        <div className="agent-capabilities">
          <span><ShieldCheck size={14} /> 人审写操作</span>
          <span><Terminal size={14} /> 工具有权限边界</span>
        </div>
      </section>

      <section className="support-layout">
        <div className="panel chat-panel">
          <div className="chat-head">
            <div className="chat-agent-avatar"><Bot size={20} /></div>
            <div><strong>Nebula Agent</strong><span><i /> 在线 · 已连接生产上下文</span></div>
            <span className="chat-model">reasoning / v2</span>
          </div>

          <div className="chat-thread">
            {messages.map((message) => (
              <div className={`chat-message ${message.role}`} key={message.id}>
                {message.role === 'agent' && <div className="chat-mini-avatar"><Sparkles size={14} /></div>}
                <div className="chat-bubble-wrap">
                  <div className="chat-bubble">{message.content}</div>
                  {message.meta && <span className="chat-meta">{message.meta}</span>}
                </div>
              </div>
            ))}
            {typing && (
              <div className="chat-message agent">
                <div className="chat-mini-avatar"><Sparkles size={14} /></div>
                <div className="typing"><span /><span /><span /></div>
              </div>
            )}
            <div ref={chatEndRef} />
          </div>

          <div className="suggestion-row">
            {suggestions.map((suggestion) => (
              <button key={suggestion} onClick={() => setInput(suggestion)}>{suggestion}</button>
            ))}
          </div>

          <form className="chat-composer" onSubmit={send}>
            <input
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="问订单、实例或退款政策…"
              aria-label="发送消息"
            />
            <button className="send-button" type="submit" disabled={!input.trim() || typing} aria-label="发送">
              <Send size={17} />
            </button>
          </form>
        </div>

        <aside className="support-context">
          <div className="panel context-panel">
            <div className="context-head"><span className="eyebrow">Live context</span><span className="context-live"><i /> Synced</span></div>
            <div className="context-order">
              <span className="context-label">当前订单</span>
              <strong>{currentOrder?.id ?? 'ORD-2408-1024'}</strong>
              <p>{currentOrder?.gpuName ?? 'NVIDIA H100 SXM'} · {currentOrder?.quantity ?? 2} GPU</p>
              <div className="context-status"><span className="status-badge status-green"><i style={{ background: '#b8f36a' }} />运行中</span><span>已运行 02:18</span></div>
            </div>
            <div className="context-metrics">
              <div><span>支付</span><strong><CheckCircle2 size={14} /> 成功</strong></div>
              <div><span>配额</span><strong><CheckCircle2 size={14} /> 已锁定</strong></div>
              <div><span>实例</span><strong><CheckCircle2 size={14} /> 健康</strong></div>
            </div>
          </div>

          <div className="panel tools-panel">
            <div className="context-head"><span className="eyebrow">Available tools</span></div>
            <div className="tool-list">
              <div><span className="tool-icon"><FileText size={15} /></span><div><strong>get_order</strong><p>查询订单与状态机</p></div><span className="tool-safe">READ</span></div>
              <div><span className="tool-icon"><Terminal size={15} /></span><div><strong>get_provision_logs</strong><p>读取开通日志</p></div><span className="tool-safe">READ</span></div>
              <div><span className="tool-icon"><Wrench size={15} /></span><div><strong>create_refund</strong><p>发起退款</p></div><span className="tool-approval">APPROVAL</span></div>
            </div>
          </div>
        </aside>
      </section>
    </div>
  )
}
