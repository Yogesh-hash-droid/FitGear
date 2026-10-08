import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ReviewForm from './ReviewForm'
import ReviewSummary from './ReviewSummary'
import StarRating from './StarRating'
import { errorMessage, fetchMyReview, fetchReviewSummary, fetchReviews } from '../services/api'
import { useAuth } from '../context/useAuth'
import { formatDate } from '../utils/format'

const PAGE_SIZE = 10
const NEWEST_FIRST = { page: 0, size: PAGE_SIZE, sort: 'createdAt,desc' }

/**
 * Ratings and reviews for one product: aggregate score, the customer's own
 * review (editable when signed in) and the review list.
 *
 * Mount with a `key` of the product id so everything re-seeds on navigation.
 * State updates only ever happen in async callbacks or event handlers, never
 * synchronously inside the effect.
 */
export default function ProductReviews({ product }) {
  const productId = product.id
  const { user } = useAuth()

  const [summary, setSummary] = useState(null)
  const [items, setItems] = useState([])
  const [hasMore, setHasMore] = useState(false)
  const [page, setPage] = useState(0)
  const [myReview, setMyReview] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    Promise.allSettled([
      fetchReviewSummary(productId),
      fetchReviews(productId, NEWEST_FIRST),
    ])
      .then(([summaryResult, reviewsResult]) => {
        if (!active) return
        if (summaryResult.status === 'fulfilled') setSummary(summaryResult.value)
        if (reviewsResult.status === 'fulfilled') {
          setItems(reviewsResult.value.content || [])
          setHasMore(!reviewsResult.value.last)
        } else {
          setError(errorMessage(reviewsResult.reason, 'Could not load reviews.'))
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [productId])

  // Only fetch the caller's own review when there is a caller.
  useEffect(() => {
    if (!user) return undefined
    let active = true
    fetchMyReview(productId)
      .then((review) => {
        if (active) setMyReview(review)
      })
      .catch(() => {})
    return () => {
      active = false
    }
  }, [user, productId])

  async function refresh() {
    const [summaryResult, reviewsResult] = await Promise.allSettled([
      fetchReviewSummary(productId),
      fetchReviews(productId, NEWEST_FIRST),
    ])
    if (summaryResult.status === 'fulfilled') setSummary(summaryResult.value)
    if (reviewsResult.status === 'fulfilled') {
      setItems(reviewsResult.value.content || [])
      setHasMore(!reviewsResult.value.last)
      setError('')
    }
    setPage(0)
  }

  async function loadMore() {
    const next = page + 1
    try {
      const data = await fetchReviews(productId, { ...NEWEST_FIRST, page: next })
      setItems((current) => [...current, ...(data.content || [])])
      setHasMore(!data.last)
      setPage(next)
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not load more reviews.'))
    }
  }

  const total = Number(summary?.totalReviews || 0)

  return (
    <section className="reviews-section" aria-labelledby="reviews-heading">
      <div className="reviews-head">
        <div>
          <p className="eyebrow">Customer feedback</p>
          <h2 id="reviews-heading">Ratings &amp; reviews</h2>
        </div>
        {total > 0 && (
          <div className="reviews-head-score">
            <StarRating value={Number(summary?.averageRating || 0)} />
            <span>{Number(summary?.averageRating || 0).toFixed(1)} · {total} review{total === 1 ? '' : 's'}</span>
          </div>
        )}
      </div>

      {loading ? (
        <p className="empty-state">Loading reviews…</p>
      ) : (
        <div className="reviews-layout">
          <aside className="reviews-aside">
            <ReviewSummary summary={summary} />
            {!user && (
              <div className="review-signin">
                <p>Own this equipment? Share how it holds up.</p>
                <Link className="clear-filter" to="/login" state={{ from: `/products/${productId}` }}>
                  Sign in to write a review →
                </Link>
              </div>
            )}
          </aside>

          <div className="reviews-main">
            {user && (
              <ReviewForm
                key={myReview?.id ?? 'new-review'}
                productId={productId}
                existing={myReview}
                onSaved={(saved) => {
                  setMyReview(saved)
                  refresh()
                }}
                onDeleted={() => {
                  setMyReview(null)
                  refresh()
                }}
              />
            )}

            {error && <p className="form-error" role="alert">{error}</p>}

            {items.length === 0 ? (
              <p className="empty-state">
                No reviews yet. Be the first to rate {product.name}.
              </p>
            ) : (
              <ul className="review-list">
                {items.map((review) => (
                  <li key={review.id} className="review-item">
                    <div className="review-item-head">
                      <div>
                        <StarRating value={Number(review.rating)} />
                        <strong>{review.title || `${review.rating} out of 5`}</strong>
                      </div>
                      <time dateTime={review.createdAt}>{formatDate(review.createdAt)}</time>
                    </div>
                    {review.body && <p className="review-body">{review.body}</p>}
                    <p className="review-author">
                      {review.userName || 'Verified customer'}
                      {myReview?.id === review.id && <span className="review-yours">Your review</span>}
                    </p>
                  </li>
                ))}
              </ul>
            )}

            {hasMore && (
              <button className="clear-filter" type="button" onClick={loadMore}>
                Show more reviews →
              </button>
            )}
          </div>
        </div>
      )}
    </section>
  )
}
