import { useState } from 'react'
import StarRating from './StarRating'
import { errorMessage, submitReview, deleteReview } from '../services/api'
import { useToast } from '../context/useToast'

const RATING_HINTS = {
  1: 'Poor',
  2: 'Not great',
  3: 'Okay',
  4: 'Good',
  5: 'Excellent',
}

/**
 * Create/edit form for the signed-in customer's own review. The server
 * upserts, so posting twice updates rather than duplicates.
 *
 * Mount it with a `key` derived from the review id so the fields are
 * re-seeded whenever the review identity changes.
 */
export default function ReviewForm({ productId, existing, onSaved, onDeleted }) {
  const [rating, setRating] = useState(existing?.rating ?? 5)
  const [title, setTitle] = useState(existing?.title ?? '')
  const [body, setBody] = useState(existing?.body ?? '')
  const [saving, setSaving] = useState(false)
  const [removing, setRemoving] = useState(false)
  const [error, setError] = useState('')
  const { push } = useToast()

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const saved = await submitReview(productId, {
        rating,
        title: title.trim(),
        body: body.trim(),
      })
      push(existing ? 'Your review was updated.' : 'Thanks — your review is live.', 'success')
      onSaved?.(saved)
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not save your review.'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!existing) return
    setRemoving(true)
    setError('')
    try {
      await deleteReview(existing.id)
      push('Your review was removed.', 'success')
      onDeleted?.(existing)
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not remove your review.'))
    } finally {
      setRemoving(false)
    }
  }

  return (
    <form className="review-form" onSubmit={handleSubmit}>
      <div className="review-form-head">
        <h3>{existing ? 'Edit your review' : 'Write a review'}</h3>
        <p>{existing ? 'Updating replaces your previous review.' : 'Only customers who sign in can post.'}</p>
      </div>

      <div className="review-form-rating">
        <span className="review-form-label">Your rating</span>
        <StarRating value={rating} interactive size="lg" onChange={setRating} />
        <span className="rating-hint">{RATING_HINTS[rating]}</span>
      </div>

      <label className="review-field">
        Title <small>optional · 120 characters</small>
        <input
          type="text"
          maxLength={120}
          value={title}
          placeholder="Sums it up in a few words"
          onChange={(event) => setTitle(event.target.value)}
        />
      </label>

      <label className="review-field">
        Your review <small>optional · 2000 characters</small>
        <textarea
          rows={5}
          maxLength={2000}
          value={body}
          placeholder="How did it hold up in training?"
          onChange={(event) => setBody(event.target.value)}
        />
      </label>

      {error && <p className="form-error" role="alert">{error}</p>}

      <div className="review-form-actions">
        <button className="primary-button" type="submit" disabled={saving || removing}>
          {saving ? 'Saving…' : existing ? 'Update review' : 'Post review'}
        </button>
        {existing && (
          <button
            className="clear-filter"
            type="button"
            disabled={saving || removing}
            onClick={handleDelete}
          >
            {removing ? 'Removing…' : 'Delete my review'}
          </button>
        )}
      </div>
    </form>
  )
}
