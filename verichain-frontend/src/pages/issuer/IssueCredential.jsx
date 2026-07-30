import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import DashboardShell from '../../components/DashboardShell.jsx'
import Alert from '../../components/Alert.jsx'
import Spinner from '../../components/Spinner.jsx'
import { ISSUER_NAV } from './nav.js'
import api, { extractErrorMessage } from '../../lib/api'

const CREDENTIAL_TYPES = [
  { value: 'INTERNSHIP_CERTIFICATE', label: 'Internship certificate' },
  { value: 'DEGREE', label: 'Degree' },
  { value: 'OFFER_LETTER', label: 'Offer letter' },
  { value: 'COURSE_COMPLETION', label: 'Course completion' },
]

const EMPTY_FORM = {
  holderName: '', holderEmail: '', credentialType: 'INTERNSHIP_CERTIFICATE',
  title: '', description: '', issueDate: '', expiryDate: '',
}

export default function IssueCredential() {
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)
  const [statusLoading, setStatusLoading] = useState(true)
  const [validationError, setValidationError] = useState('')
  const [issuerProfile, setIssuerProfile] = useState(null)
  const navigate = useNavigate()

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }))
  }

  useEffect(() => {
    api.get('/api/issuer/profile')
      .then(({ data }) => setIssuerProfile(data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setStatusLoading(false))
  }, [])

  function validate() {
    if (!form.holderName.trim()) return 'Holder name is required.'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.holderEmail)) return 'Enter a valid holder email.'
    if (!form.title.trim()) return 'Title is required.'
    if (!form.issueDate) return 'Issue date is required.'
    if (form.expiryDate && form.expiryDate < form.issueDate) return 'Expiry date cannot be earlier than the issue date.'
    if (form.description.length > 1000) return 'Description must be at most 1000 characters.'
    return ''
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')

    if (issuerProfile?.approvalStatus !== 'APPROVED') {
      setError('Your issuer account must be approved before you can issue credentials.')
      return
    }

    const localValidationError = validate()
    setValidationError(localValidationError)
    if (localValidationError) return

    setLoading(true)
    try {
      const payload = {
        ...form,
        holderName: form.holderName.trim(),
        holderEmail: form.holderEmail.trim(),
        title: form.title.trim(),
        description: form.description.trim(),
        expiryDate: form.expiryDate || null,
      }
      const { data } = await api.post('/api/issuer/credentials', payload)
      setForm(EMPTY_FORM)
      setSuccess('Credential issued successfully. You can view or share it from your credentials list.')
      navigate(`/issuer/credentials/${data.id}`)
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <DashboardShell title="Issue a credential" subtitle="This will be sealed into your organization's hash-chain and signed" navItems={ISSUER_NAV}>
      {statusLoading && <div className="mb-4 flex items-center gap-2 text-ink-soft"><Spinner /> Checking issuer status…</div>}
      {error && <div className="mb-4"><Alert variant="error">{error}</Alert></div>}
      {success && <div className="mb-4"><Alert variant="success">{success}</Alert></div>}
      {validationError && <div className="mb-4"><Alert variant="error">{validationError}</Alert></div>}

      {!statusLoading && issuerProfile?.approvalStatus !== 'APPROVED' && (
        <Alert variant="info" className="mb-4">
          Your issuer account is currently {issuerProfile?.approvalStatus?.toLowerCase() || 'pending'}. Approval is required before credentials can be issued.
        </Alert>
      )}

      <form onSubmit={handleSubmit} className="card p-6 space-y-5 max-w-xl">
        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label className="field-label">Holder name</label>
            <input required className="field-input" value={form.holderName}
              onChange={(e) => update('holderName', e.target.value)} />
          </div>
          <div>
            <label className="field-label">Holder email</label>
            <input type="email" required className="field-input" value={form.holderEmail}
              onChange={(e) => update('holderEmail', e.target.value)} />
          </div>
        </div>

        <div>
          <label className="field-label">Credential type</label>
          <select className="field-input" value={form.credentialType}
            onChange={(e) => update('credentialType', e.target.value)}>
            {CREDENTIAL_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
          </select>
        </div>

        <div>
          <label className="field-label">Title</label>
          <input required placeholder="e.g. Backend Development Intern" className="field-input" value={form.title}
            onChange={(e) => update('title', e.target.value)} />
        </div>

        <div>
          <label className="field-label">Description <span className="normal-case text-ink-faint">(optional)</span></label>
          <textarea rows={3} className="field-input resize-none" value={form.description}
            onChange={(e) => update('description', e.target.value)} />
        </div>

        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label className="field-label">Issue date</label>
            <input type="date" required className="field-input" value={form.issueDate}
              onChange={(e) => update('issueDate', e.target.value)} />
          </div>
          <div>
            <label className="field-label">Expiry date <span className="normal-case text-ink-faint">(optional)</span></label>
            <input type="date" className="field-input" value={form.expiryDate}
              onChange={(e) => update('expiryDate', e.target.value)} />
          </div>
        </div>

        <button type="submit" disabled={loading || issuerProfile?.approvalStatus !== 'APPROVED'} className="btn-bronze">
          {loading ? <Spinner /> : 'Issue & sign credential'}
        </button>
      </form>
    </DashboardShell>
  )
}
