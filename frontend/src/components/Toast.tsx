import { CheckCircle2, Info, TriangleAlert, X } from 'lucide-react'
import type { ToastMessage } from '../types'

interface ToastProps {
  toast: ToastMessage | null
  onClose: () => void
}

const icons = {
  success: <CheckCircle2 size={18} />,
  info: <Info size={18} />,
  warning: <TriangleAlert size={18} />,
}

export default function Toast({ toast, onClose }: ToastProps) {
  if (!toast) return null

  return (
    <div className={`toast toast-${toast.tone}`} role="status">
      <span className="toast-icon">{icons[toast.tone]}</span>
      <div>
        <strong>{toast.title}</strong>
        <p>{toast.description}</p>
      </div>
      <button onClick={onClose} aria-label="关闭提示">
        <X size={15} />
      </button>
    </div>
  )
}
