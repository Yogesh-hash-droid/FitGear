import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(form)
      navigate(location.state?.from || '/')
    } catch {
      setError('We could not sign you in. Check your email and password.')
    } finally {
      setSubmitting(false)
    }
  }

  return <main className="auth-page site-shell"><nav className="nav-bar"><Link className="brand" to="/"><span>F</span> FITGEAR</Link><Link className="back-link" to="/">Back to home</Link></nav><section className="auth-card"><p className="eyebrow">WELCOME BACK</p><h1>Return to<br /><em>the work.</em></h1><form onSubmit={handleSubmit}><label>Email<input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required /></label><label>Password<input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required /></label>{error && <p className="form-error">{error}</p>}<button className="primary-button" disabled={submitting}>{submitting ? 'Signing in...' : 'Sign in'} <span>↗</span></button></form><p className="auth-switch">New to FitGear? <Link to="/register">Create an account</Link></p></section></main>
}
