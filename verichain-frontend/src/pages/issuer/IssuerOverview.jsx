import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import DashboardShell from '../../components/DashboardShell.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import Alert from '../../components/Alert.jsx'
import Spinner from '../../components/Spinner.jsx'
import { ISSUER_NAV } from './nav.js'
import api, { extractErrorMessage } from '../../lib/api'

export default function IssuerOverview() {
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/issuer/profile')
      .then(({ data }) => setProfile(data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  return (
    <DashboardShell title="Overview" subtitle="Your organization's issuer profile" navItems={ISSUER_NAV}>
      {loading && <div className="flex items-center gap-2 text-ink-soft"><Spinner /> Loading…</div>}
      {error && <Alert variant="error">{error}</Alert>}

      {profile && (
        <div className="space-y-6">
          {profile.approvalStatus === 'PENDING' && (
            <Alert variant="info">
              Your account is awaiting admin approval. You won't be able to issue credentials until it's approved.
            </Alert>
          )}
          {profile.approvalStatus === 'REJECTED' && (
            <Alert variant="error">Your issuer application was rejected. Contact the platform admin for details.</Alert>
          )}

          <div className="card p-6">
            <div className="flex items-start justify-between mb-6">
              <div>
                <h2 className="font-display text-lg font-semibold">{profile.organizationName}</h2>
                {profile.registrationNumber && (
                  <p className="text-sm text-ink-faint">Reg. no. {profile.registrationNumber}</p>
                )}
              </div>
              <StatusBadge status={profile.approvalStatus} />
            </div>

            {profile.approvalStatus === 'APPROVED' && (
              <div className="space-y-4">
                <div>
                  <p className="field-label">Public key (RSA-2048)</p>
                  <p className="font-mono text-xs text-ink-soft bg-paper-dim rounded-sm p-3 break-all leading-relaxed">
                    {profile.publicKey}
                  </p>
                  <p className="text-xs text-ink-faint mt-2">
                    Anyone verifying your credentials checks your signature against this key. Your private key never leaves the server and is encrypted at rest.
                  </p>
                </div>

                <div className="rounded-sm border border-line bg-paper-dim p-4">
                  <h3 className="font-display text-base font-semibold text-ink mb-2">Next steps</h3>
                  <div className="flex flex-wrap gap-2">
                    <Link to="/issuer/issue" className="btn-primary">Issue a credential</Link>
                    <Link to="/issuer/credentials" className="btn-ghost">View issued credentials</Link>
                    <Link to="/" className="btn-ghost">Verify a credential</Link>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </DashboardShell>
  )
}
