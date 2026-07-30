import React, { useEffect, useState } from 'react'
import DashboardShell from '../../components/DashboardShell.jsx'
import Alert from '../../components/Alert.jsx'
import Spinner from '../../components/Spinner.jsx'
import { ADMIN_NAV } from './nav.js'
import api, { extractErrorMessage } from '../../lib/api'

export default function PendingIssuers() {
  const [issuers, setIssuers] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [actingOn, setActingOn] = useState(null)

  function load() {
    setLoading(true)
    api.get('/api/admin/issuers/pending')
      .then(({ data }) => setIssuers(data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function handleDecision(issuerId, decision) {
    setActingOn(issuerId)
    setError('')
    try {
      await api.put(`/api/admin/issuers/${issuerId}/${decision}`)
      load()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setActingOn(null)
    }
  }

  return (
    <DashboardShell title="Pending issuers" subtitle="Approving generates that organization's signing keypair" navItems={ADMIN_NAV}>
      {loading && <div className="flex items-center gap-2 text-ink-soft"><Spinner /> Loading…</div>}
      {error && <div className="mb-4"><Alert variant="error">{error}</Alert></div>}

      {!loading && issuers.length === 0 && (
        <div className="card p-10 text-center text-ink-soft text-sm">No issuer applications waiting for review.</div>
      )}

      <div className="space-y-3">
        {issuers.map((issuer) => (
          <div key={issuer.issuerId} className="card p-5 flex items-center justify-between gap-4">
            <div>
              <p className="font-medium text-ink">{issuer.organizationName}</p>
              {issuer.registrationNumber && <p className="text-xs text-ink-faint">Reg. no. {issuer.registrationNumber}</p>}
              <p className="text-sm text-ink-soft mt-1">{issuer.contactName} &middot; {issuer.contactEmail}</p>
            </div>
            <div className="flex gap-2 shrink-0">
              <button
                onClick={() => handleDecision(issuer.issuerId, 'approve')}
                disabled={actingOn === issuer.issuerId}
                className="btn-primary"
              >
                {actingOn === issuer.issuerId ? <Spinner /> : 'Approve'}
              </button>
              <button
                onClick={() => handleDecision(issuer.issuerId, 'reject')}
                disabled={actingOn === issuer.issuerId}
                className="btn-ghost"
              >
                Reject
              </button>
            </div>
          </div>
        ))}
      </div>
    </DashboardShell>
  )
}
