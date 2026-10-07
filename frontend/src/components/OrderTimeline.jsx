import { STATUS_FLOW, STATUS_LABELS } from '../utils/format'

/**
 * Vertical lifecycle timeline for an order.
 * Completed steps are filled, the current step is highlighted,
 * pending steps are dim. A cancelled order shows a terminal note.
 */
export default function OrderTimeline({ status }) {
  if (status === 'CANCELLED') {
    return (
      <div className="timeline timeline-cancelled">
        <div className="timeline-step done">
          <span className="timeline-dot" />
          <div>
            <strong>Cancelled</strong>
            <p>This order was cancelled. Any payment is refunded to the original method.</p>
          </div>
        </div>
      </div>
    )
  }

  const currentIndex = STATUS_FLOW.indexOf(status)

  return (
    <ol className="timeline">
      {STATUS_FLOW.map((step, index) => {
        const state = index < currentIndex ? 'done' : index === currentIndex ? 'current' : 'pending'
        return (
          <li key={step} className={`timeline-step ${state}`}>
            <span className="timeline-dot" />
            <div>
              <strong>{STATUS_LABELS[step]}</strong>
              {state === 'current' && <p>Current status</p>}
              {state === 'done' && <p>Completed</p>}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
