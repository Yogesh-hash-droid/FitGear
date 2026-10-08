/**
 * Star rating, read-only or interactive.
 *
 * Read-only mode paints a fractional fill over a five-star track so an
 * average such as 4.3 renders as four full stars and a sliver of the fifth.
 * Interactive mode renders five buttons instead.
 */
export default function StarRating({
  value = 0,
  interactive = false,
  onChange,
  size = 'md',
  label,
}) {
  const safe = Math.max(0, Math.min(5, Number(value) || 0))
  const text = label ?? `${safe.toFixed(1)} out of 5 stars`

  if (interactive) {
    return (
      <div className={`star-picker star-picker-${size}`}>
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            className={star <= safe ? 'on' : ''}
            aria-pressed={star === safe}
            aria-label={`Rate ${star} star${star > 1 ? 's' : ''}`}
            onClick={() => onChange?.(star)}
          >
            ★
          </button>
        ))}
      </div>
    )
  }

  const percent = (safe / 5) * 100

  return (
    <span className={`stars stars-${size}`} role="img" aria-label={text} title={text}>
      <span className="stars-track" aria-hidden="true">★★★★★</span>
      <span className="stars-fill" style={{ width: `${percent}%` }} aria-hidden="true">
        ★★★★★
      </span>
    </span>
  )
}
