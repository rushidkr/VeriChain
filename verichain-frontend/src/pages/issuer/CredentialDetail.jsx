import React, { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import DashboardShell from '../../components/DashboardShell.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import ChainLedger from '../../components/ChainLedger.jsx'
import Alert from '../../components/Alert.jsx'
import Spinner from '../../components/Spinner.jsx'
import { ISSUER_NAV } from './nav.js'
import api, { API_BASE_URL, extractErrorMessage } from '../../lib/api'

export default function CredentialDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [credential, setCredential] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [revoking, setRevoking] = useState(false)
  const [showRevokeForm, setShowRevokeForm] = useState(false)
  const [reason, setReason] = useState('')

  function load() {
    setLoading(true)
    api.get(`/api/issuer/credentials/${id}`)
      .then(({ data }) => setCredential(data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false))
  }

  useEffect(load, [id])

  async function handleRevoke() {
    setRevoking(true)
    setError('')
    try {
      await api.put(`/api/issuer/credentials/${id}/revoke`, { reason })
      setShowRevokeForm(false)
      load()
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setRevoking(false)
    }
  }

  return (
    <DashboardShell title="Credential detail" navItems={ISSUER_NAV}>
      <Link to="/issuer/credentials" className="text-sm text-ink-soft hover:text-ink mb-6 inline-block">&larr; Back to credentials</Link>

      {loading && <div className="flex items-center gap-2 text-ink-soft"><Spinner /> Loading…</div>}
      {error && <div className="mb-4"><Alert variant="error">{error}</Alert></div>}

      {credential && (
        <div className="grid md:grid-cols-3 gap-6">
          <div className="md:col-span-2 card p-6 space-y-5">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="font-display text-lg font-semibold">{credential.title}</h2>
                <p className="text-sm text-ink-soft">{credential.holderName} &middot; {credential.holderEmail}</p>
              </div>
              <StatusBadge status={credential.status} />
            </div>

            {credential.description && <p className="text-sm text-ink-soft leading-relaxed">{credential.description}</p>}

            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="field-label">Issue date</p>
                <p className="font-mono text-xs">{credential.issueDate}</p>
              </div>
              <div>
                <p className="field-label">Expiry date</p>
                <p className="font-mono text-xs">{credential.expiryDate || '\u2014'}</p>
              </div>
            </div>

            <div>
              <p className="field-label">Data hash</p>
              <ChainLedger hash={credential.dataHash} state="neutral" segments={8} compact />
            </div>
            <div>
              <p className="field-label">Chain hash (this record's link)</p>
              <ChainLedger hash={credential.chainHash} state="verified" segments={8} compact />
            </div>

            {credential.status === 'ACTIVE' && (
              <div className="pt-4 border-t border-line-soft">
                {!showRevokeForm ? (
                  <button onClick={() => setShowRevokeForm(true)} className="btn-danger">Revoke credential</button>
                ) : (
                  <div className="space-y-3">
                    <div>
                      <label className="field-label">Reason for revocation</label>
                      <input className="field-input" value={reason} onChange={(e) => setReason(e.target.value)}
                        placeholder="e.g. Issued in error" />
                    </div>
                    <div className="flex gap-2">
                      <button onClick={handleRevoke} disabled={revoking} className="btn-danger">
                        {revoking ? <Spinner /> : 'Confirm revoke'}
                      </button>
                      <button onClick={() => setShowRevokeForm(false)} className="btn-ghost">Cancel</button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="card p-6 text-center flex flex-col items-center">
            <p className="field-label mb-3">Verification QR</p>
            <img
              src={`${API_BASE_URL}/api/verify/${credential.id}/qrcode`}
              alt="Verification QR code"
              className="w-full border border-line rounded-sm mb-3"
            />
            <a
              href={`/verify/${credential.id}`}
              target="_blank" rel="noreferrer"
              className="text-xs text-bronze hover:underline break-all mb-4"
            >
              {credential.verificationUrl}
            </a>
            <a
              href={`${API_BASE_URL}/api/verify/${credential.id}/pdf`}
              download={`certificate-${credential.id}.pdf`}
              className="btn-primary w-full text-center"
            >
              Download PDF
            </a>
          </div>
        </div>
      )}
    </DashboardShell>
  )
}
