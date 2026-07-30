import React, { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import PublicHeader from '../components/PublicHeader.jsx'
import SealStamp from '../components/SealStamp.jsx'
import ChainLedger from '../components/ChainLedger.jsx'
import Spinner from '../components/Spinner.jsx'
import api, { API_BASE_URL, extractErrorMessage } from '../lib/api'

const CHAIN_STATE_BY_RESULT = {
  VALID: 'verified',
  TAMPERED: 'tampered',
  REVOKED: 'revoked',
  EXPIRED: 'expired',
  NOT_FOUND: 'neutral',
}

export default function VerifyResult() {
  const { credentialId } = useParams()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')

    api.get(`/api/verify/${credentialId}`)
      .then(({ data }) => { if (!cancelled) setData(data) })
      .catch((err) => { if (!cancelled) setError(extractErrorMessage(err)) })
      .finally(() => { if (!cancelled) setLoading(false) })

    return () => { cancelled = true }
  }, [credentialId])

  return (
    <div className="min-h-screen flex flex-col">
      <PublicHeader />

      <main className="flex-1 max-w-2xl w-full mx-auto px-6 py-16">
        <div className="mb-8">
          <p className="text-xs uppercase tracking-[0.2em] text-bronze font-medium mb-2">Verification result</p>
          <p className="font-mono text-xs text-ink-faint break-all">{credentialId}</p>
        </div>

        {loading && (
          <div className="flex items-center gap-3 text-ink-soft py-16 justify-center">
            <Spinner /> Checking the chain…
          </div>
        )}

        {!loading && error && (
          <div className="card p-8 text-center">
            <p className="text-alert text-sm">{error}</p>
          </div>
        )}

        {!loading && !error && data && (
          <div className="card p-10 flex flex-col items-center text-center">
            <SealStamp result={data.result} />

            <p className="text-sm text-ink-soft mt-6 max-w-sm">{data.message}</p>

            {data.result !== 'NOT_FOUND' && (
              <div className="w-full mt-8 pt-8 border-t border-line-soft text-left space-y-4">
                <Row label="Holder" value={data.holderName} />
                <Row label="Issued by" value={data.issuerOrganization} />
                <Row label="Credential" value={data.title} />
                <Row label="Type" value={data.credentialType?.replace(/_/g, ' ')} />
                <Row label="Issue date" value={data.issueDate} />
                {data.expiryDate && <Row label="Expires" value={data.expiryDate} />}

                <div className="pt-2">
                  <p className="field-label">Chain integrity</p>
                  <ChainLedger
                    hash={credentialId.replace(/-/g, '')}
                    state={CHAIN_STATE_BY_RESULT[data.result]}
                    segments={8}
                  />
                  <p className="text-xs text-ink-faint mt-2">
                    {data.chainIntact ? 'Content hash matches the issued record.' : 'Content hash does NOT match — data was altered.'}
                    {' '}{data.signatureValid ? 'Issuer signature verified.' : 'Issuer signature could not be verified.'}
                  </p>
                </div>

                <div className="pt-4 flex flex-col items-center gap-4">
                  <img
                    src={`${API_BASE_URL}/api/verify/${credentialId}/qrcode`}
                    alt="QR code for this credential"
                    className="w-32 h-32 border border-line rounded-sm"
                  />
                  <a
                    href={`${API_BASE_URL}/api/verify/${credentialId}/pdf`}
                    download={`certificate-${credentialId}.pdf`}
                    className="btn-primary inline-flex items-center gap-2"
                  >
                    Download PDF Certificate
                  </a>
                </div>
              </div>
            )}
          </div>
        )}

        {!loading && !error && data && data.result === 'NOT_FOUND' && (
          <p className="text-center text-sm text-ink-faint mt-4">
            Have the physical certificate? <Link to="/verify-upload" className="text-bronze hover:underline">Try uploading it instead</Link>
          </p>
        )}

        <p className="text-center mt-8">
          <Link to="/" className="text-sm text-ink-soft hover:text-ink transition-colors">
            &larr; Verify another credential
          </Link>
        </p>
      </main>
    </div>
  )
}

function Row({ label, value }) {
  if (!value) return null
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-xs uppercase tracking-wide text-ink-faint shrink-0">{label}</span>
      <span className="text-sm text-ink text-right">{value}</span>
    </div>
  )
}
