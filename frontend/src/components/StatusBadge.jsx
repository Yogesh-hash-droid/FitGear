import { STATUS_LABELS } from '../utils/format'

export default function StatusBadge({ status }) {
  if (!status) return null
  return (
    <span className={`status-badge status-${status.toLowerCase()}`}>
      {STATUS_LABELS[status] || status}
    </span>
  )
}
