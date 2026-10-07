import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await register(form)
      navigate('/')
    } catch {
      setError('That email may already be registered, or the details are invalid.')
    } finally {
      setSubmitting(false)
    }
  }

  return <main className="auth-page site-shell"><nav className="nav-bar"><Link className="brand" to="/"><span>F</span> FITGEAR</Link><Link className="back-link" to="/">Back to home</Link></nav><section className="auth-card"><p className="eyebrow">START HERE</p><h1>Make room<br /><em>to move.</em></h1><form onSubmit={handleSubmit}><label>Name<input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required /></label><label>Email<input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required /></label><label>Password<input type="password" minLength="8" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required /></label>{error && <p className="form-error">{error}</p>}<button className="primary-button" disabled={submitting}>{submitting ? 'Creating...' : 'Create account'} <span>↗</span></button></form><p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p></section></main>
}
