import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import PublicHeader from '../components/PublicHeader.jsx'
import Alert from '../components/Alert.jsx'
import Spinner from '../components/Spinner.jsx'
import { useAuth } from '../context/AuthContext.jsx'
import { extractErrorMessage } from '../lib/api'

export default function Register() {
  const { registerStudent, registerIssuer } = useAuth()
  const navigate = useNavigate()
  const [role, setRole] = useState('STUDENT')
  const [form, setForm] = useState({
    name: '', email: '', password: '', organizationName: '', registrationNumber: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [validationError, setValidationError] = useState('')

  function validate() {
    if (!form.name.trim()) return 'Full name is required.'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return 'Enter a valid email address.'
    if (form.password.length < 8) return 'Password must be at least 8 characters.'
    if (!/[A-Z]/.test(form.password) || !/[a-z]/.test(form.password) || !/\d/.test(form.password)) {
      return 'Password must include uppercase, lowercase, and a number.'
    }
    if (role === 'ISSUER' && (!form.organizationName.trim() || form.organizationName.length > 120)) {
      return 'Organization name is required and must be under 120 characters.'
    }
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
      if (role === 'STUDENT') {
        await registerStudent({ name: form.name.trim(), email: form.email.trim(), password: form.password })
        navigate('/student')
      } else {
        const user = await registerIssuer({ ...form, name: form.name.trim(), organizationName: form.organizationName.trim(), registrationNumber: form.registrationNumber.trim() })
        navigate('/issuer')
      }
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
          <h1 className="text-2xl font-display font-semibold text-center mb-1">Create an account</h1>
          <p className="text-sm text-ink-soft text-center mb-6">
            {role === 'STUDENT' ? 'For credential holders' : 'For institutions that issue credentials'}
          </p>

          <div className="flex rounded-sm border border-line p-1 mb-6 bg-white">
            {['STUDENT', 'ISSUER'].map((r) => (
              <button
                key={r}
                type="button"
                onClick={() => setRole(r)}
                className={`flex-1 py-2 text-sm rounded-sm transition-colors ${
                  role === r ? 'bg-ink text-paper' : 'text-ink-soft hover:bg-paper-dim'
                }`}
              >
                {r === 'STUDENT' ? 'I hold credentials' : 'I issue credentials'}
              </button>
            ))}
          </div>

          {error && <div className="mb-4"><Alert variant="error">{error}</Alert></div>}
          {validationError && <div className="mb-4"><Alert variant="error">{validationError}</Alert></div>}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="field-label">{role === 'ISSUER' ? 'Contact name' : 'Full name'}</label>
              <input required className="field-input" value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })} />
            </div>
            <div>
              <label className="field-label">Email</label>
              <input type="email" required className="field-input" value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <div>
              <label className="field-label">Password</label>
              <input type="password" required minLength={8} className="field-input" value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })} />
            </div>

            {role === 'ISSUER' && (
              <>
                <div>
                  <label className="field-label">Organization name</label>
                  <input required className="field-input" value={form.organizationName}
                    onChange={(e) => setForm({ ...form, organizationName: e.target.value })} />
                </div>
                <div>
                  <label className="field-label">Registration number <span className="normal-case text-ink-faint">(optional)</span></label>
                  <input className="field-input" value={form.registrationNumber}
                    onChange={(e) => setForm({ ...form, registrationNumber: e.target.value })} />
                </div>
                <Alert variant="info">
                  Issuer accounts require admin approval before they can issue credentials.
                </Alert>
              </>
            )}

            <button type="submit" disabled={loading} className="btn-primary w-full">
              {loading ? <Spinner /> : 'Create account'}
            </button>
          </form>

          <p className="text-sm text-ink-soft text-center mt-6">
            Already have an account? <Link to="/login" className="text-bronze hover:underline">Log in</Link>
          </p>
        </div>
      </main>
    </div>
  )
}
