import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import PublicHeader from '../components/PublicHeader.jsx'
import Alert from '../components/Alert.jsx'
import Spinner from '../components/Spinner.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { extractErrorMessage } from '../lib/api'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [validationError, setValidationError] = useState('')

  function validate() {
    if (!form.email.trim()) return 'Email is required.'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return 'Enter a valid email address.'
    if (!form.password) return 'Password is required.'
    if (form.password.length < 8) return 'Password must be at least 8 characters.'
    return ''
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const localValidationError = validate()
    setValidationError(localValidationError)
    if (localValidationError) return

    setLoading(true)
    try {
      const user = await login(form.email.trim(), form.password)
      navigate(user.role === 'ADMIN' ? '/admin' : user.role === 'ISSUER' ? '/issuer' : user.role === 'STUDENT' ? '/student' : '/')
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <PublicHeader />
      <main className="flex-1 flex items-center justify-center px-6 py-16">
        <div className="w-full max-w-sm">
          <h1 className="text-2xl font-display font-semibold text-center mb-1">Log in</h1>
          <p className="text-sm text-ink-soft text-center mb-8">Access your issuer or admin dashboard</p>

          {error && <div className="mb-4"><Alert variant="error">{error}</Alert></div>}
          {validationError && <div className="mb-4"><Alert variant="error">{validationError}</Alert></div>}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="field-label">Email</label>
              <input
                type="email" required className="field-input"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </div>
            <div>
              <label className="field-label">Password</label>
              <input
                type="password" required className="field-input"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
              />
            </div>
            <button type="submit" disabled={loading} className="btn-primary w-full">
              {loading ? <Spinner /> : 'Log in'}
            </button>
          </form>

          <p className="text-sm text-ink-soft text-center mt-6">
            Don't have an account? <Link to="/register" className="text-bronze hover:underline">Register</Link>
          </p>
        </div>
      </main>
    </div>
  )
}
