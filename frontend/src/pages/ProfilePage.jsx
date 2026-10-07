import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Footer from '../components/Footer'
import Navbar from '../components/Navbar'
import { useAuth } from '../context/useAuth'
import { useToast } from '../context/useToast'
import { errorMessage, fetchProfile, updateProfile } from '../services/api'
import { formatDate } from '../utils/format'

export default function ProfilePage() {
  const { user, logout } = useAuth()
  const { push } = useToast()
  const [profile, setProfile] = useState(null)
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    fetchProfile()
      .then((data) => {
        if (active) {
          setProfile(data)
          setName(data.name || '')
        }
      })
      .catch((requestError) => {
        if (active) setError(errorMessage(requestError, 'Could not load your profile.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      const updated = await updateProfile({ name: name.trim() })
      setProfile(updated)
      const saved = JSON.parse(localStorage.getItem('fitgear_user') || '{}')
      localStorage.setItem('fitgear_user', JSON.stringify({ ...saved, name: updated.name }))
      push('Profile updated.', 'success')
    } catch (requestError) {
      setError(errorMessage(requestError, 'Could not update your profile.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="site-shell">
      <Navbar />

      <section className="simple-page-content profile-page">
        <p className="eyebrow">YOUR ACCOUNT</p>
        <h1>Profile<br /><em>settings.</em></h1>

        {loading ? (
          <p className="empty-state">Loading profile...</p>
        ) : (
          <div className="profile-layout">
            <div className="profile-card">
              <h2>Details</h2>
              <form onSubmit={handleSubmit}>
                <label>
                  Name
                  <input required maxLength="120" value={name} onChange={(event) => setName(event.target.value)} />
                </label>
                <label>
                  Email
                  <input value={profile?.email || user?.email || ''} readOnly />
                </label>
                <label>
                  Role
                  <input value={profile?.role || user?.role || ''} readOnly />
                </label>
                {profile?.createdAt && <p className="profile-meta">Member since {formatDate(profile.createdAt)}</p>}
                {error && <p className="form-error" role="alert">{error}</p>}
                <button className="primary-button" type="submit" disabled={saving || name.trim() === profile?.name}>
                  {saving ? 'Saving…' : 'Save changes'} <span>↗</span>
                </button>
              </form>
            </div>

            <div className="profile-card">
              <h2>Activity</h2>
              <div className="profile-links">
                <Link className="history-item" to="/orders">
                  <span><strong>Order history</strong><small>Track and manage your orders</small></span>
                  <b>→</b>
                </Link>
                <Link className="history-item" to="/cart">
                  <span><strong>Shopping bag</strong><small>Review items before checkout</small></span>
                  <b>→</b>
                </Link>
                {user?.role === 'ADMIN' && (
                  <Link className="history-item" to="/admin">
                    <span><strong>Admin dashboard</strong><small>Products, orders and metrics</small></span>
                    <b>→</b>
                  </Link>
                )}
              </div>
              <button className="remove-button profile-signout" type="button" onClick={logout}>
                Sign out
              </button>
            </div>
          </div>
        )}
      </section>

      <Footer />
    </div>
  )
}
