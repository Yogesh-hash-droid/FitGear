import StarRating from './StarRating'

/**
 * Aggregate rating for a product: big average, star row and a per-star
 * histogram. Every bucket from 5 down to 1 is always rendered, including
 * the empty ones, so the bars never jump around as reviews arrive.
 */
export default function ReviewSummary({ summary }) {
  const total = Number(summary?.totalReviews || 0)
  const average = Number(summary?.averageRating || 0)

  return (
    <div className="review-summary">
      <div className="review-score">
        <strong className="review-score-value">
          {total ? average.toFixed(1) : '—'}
        </strong>
        <div>
          <StarRating value={average} size="lg" />
          <p className="review-score-count">
            {total ? `${total} review${total === 1 ? '' : 's'}` : 'No reviews yet'}
          </p>
        </div>
      </div>

      <ul className="rating-bars" aria-label="Rating breakdown">
        {[5, 4, 3, 2, 1].map((stars) => {
          const count = Number(summary?.distribution?.[stars] || 0)
          const percent = total ? Math.round((count / total) * 100) : 0
          return (
            <li key={stars}>
              <span className="bar-stars">{stars} ★</span>
              <span className="bar-track">
                <span className="bar-fill" style={{ width: `${percent}%` }} />
              </span>
              <span className="bar-count">{count}</span>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
