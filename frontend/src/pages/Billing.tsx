import { ArrowDownToLine, ArrowUpRight, CheckCircle2, CircleDollarSign, CreditCard, Download, ReceiptText, TrendingUp, Wallet } from 'lucide-react'
import { currency } from '../utils'

const invoices = [
  { id: 'INV-2026-11-001', date: '2026-11-21', amount: 8420, status: '已结算' },
  { id: 'INV-2026-10-001', date: '2026-10-31', amount: 7492, status: '已结算' },
  { id: 'INV-2026-09-001', date: '2026-09-30', amount: 6128, status: '已结算' },
]

const usage = [
  { name: 'H100 SXM', hours: 86, amount: 3612, percent: 68, color: '#b8f36a' },
  { name: 'A100 80GB', hours: 124, amount: 3224, percent: 52, color: '#65d9f7' },
  { name: 'L40S', hours: 63, amount: 1134, percent: 31, color: '#c7a8ff' },
  { name: 'RTX 4090', hours: 50, amount: 450, percent: 18, color: '#ffb86b' },
]

export default function Billing() {
  return (
    <div className="page billing-page">
      <section className="page-heading">
        <div>
          <span className="eyebrow"><ReceiptText size={13} /> Finance</span>
          <h1>账单与<em>用量。</em></h1>
          <p>透明查看每一笔算力消费，按小时拆解资源成本。</p>
        </div>
        <div className="heading-actions">
          <button className="secondary-button"><ArrowDownToLine size={16} /> 导出账单</button>
        </div>
      </section>

      <section className="billing-hero">
        <div className="balance-card">
          <div className="balance-head">
            <span><Wallet size={16} /> 账户余额</span>
            <span className="balance-status"><i /> 正常</span>
          </div>
          <strong>{currency(12680)}</strong>
          <p>预计可用 18 天 · 自动充值阈值 ¥2,000</p>
          <div className="balance-actions">
            <button className="primary-button"><CreditCard size={15} /> 充值</button>
            <button className="secondary-button">管理支付方式</button>
          </div>
          <div className="balance-card-glow" />
        </div>

        <div className="spend-card">
          <div className="spend-card-head">
            <span>本月支出</span>
            <span className="spend-up"><TrendingUp size={14} /> 12.4%</span>
          </div>
          <strong>{currency(8420)}</strong>
          <p>预算 ¥12,500 · 已使用 68%</p>
          <div className="budget-bar"><span style={{ width: '68%' }} /></div>
          <div className="budget-meta"><span>¥0</span><span>¥12,500</span></div>
        </div>

        <div className="savings-card">
          <div className="savings-icon"><CircleDollarSign size={18} /></div>
          <div>
            <span>本月可节省</span>
            <strong>{currency(1240)}</strong>
            <p>通过 72h 预留实例与闲置释放</p>
          </div>
          <ArrowUpRight size={18} className="savings-arrow" />
        </div>
      </section>

      <section className="billing-grid">
        <article className="panel usage-breakdown">
          <div className="panel-head">
            <div><span className="eyebrow">Usage breakdown</span><h2>资源用量</h2></div>
            <span className="billing-period">2026 年 11 月</span>
          </div>
          <div className="usage-list">
            {usage.map((item) => (
              <div className="usage-item" key={item.name}>
                <div className="usage-item-head">
                  <span className="usage-dot" style={{ background: item.color }} />
                  <strong>{item.name}</strong>
                  <span>{item.hours}h</span>
                  <em>{currency(item.amount)}</em>
                </div>
                <div className="usage-track"><span style={{ width: `${item.percent}%`, background: item.color }} /></div>
              </div>
            ))}
          </div>
          <div className="usage-total">
            <span>GPU 总时长</span>
            <strong>323 小时</strong>
            <span>折合 {currency(26)} / 小时</span>
          </div>
        </article>

        <article className="panel invoice-panel">
          <div className="panel-head">
            <div><span className="eyebrow">Invoices</span><h2>账单记录</h2></div>
            <button className="icon-button" aria-label="下载全部账单"><Download size={16} /></button>
          </div>
          <div className="invoice-list">
            {invoices.map((invoice) => (
              <div className="invoice-row" key={invoice.id}>
                <div className="invoice-icon"><ReceiptText size={16} /></div>
                <div><strong>{invoice.id}</strong><span>{invoice.date}</span></div>
                <div className="invoice-amount">{currency(invoice.amount)}</div>
                <span className="invoice-status"><CheckCircle2 size={13} /> {invoice.status}</span>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="panel payment-panel">
        <div className="panel-head">
          <div><span className="eyebrow">Payment method</span><h2>支付方式</h2></div>
          <button className="text-button">更换</button>
        </div>
        <div className="payment-method">
          <div className="card-brand">VISA</div>
          <div><strong>Visa •••• 4242</strong><span>有效期 08/29 · 默认支付方式</span></div>
          <span className="default-tag">默认</span>
        </div>
      </section>
    </div>
  )
}
