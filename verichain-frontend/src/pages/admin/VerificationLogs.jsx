import React, { useEffect, useState } from 'react'
import DashboardShell from '../../components/DashboardShell.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import Alert from '../../components/Alert.jsx'
import Spinner from '../../components/Spinner.jsx'
import { ADMIN_NAV } from './nav.js'
import api, { extractErrorMessage } from '../../lib/api'

const PAGE_SIZE = 20

export default function VerificationLogs() {
  const [logPage, setLogPage] = useState(null)
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    api.get('/api/admin/verification-logs', { params: { page, size: PAGE_SIZE } })
      .then(({ data }) => setLogPage(data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [page])

  return (
    <DashboardShell title="Verification logs" subtitle="Every verification attempt, successful or not" navItems={ADMIN_NAV}>
      {loading && <div className="flex items-center gap-2 text-ink-soft"><Spinner /> Loading…</div>}
      {error && <Alert variant="error">{error}</Alert>}

      {logPage && logPage.content.length === 0 && (
        <div className="card p-10 text-center text-ink-soft text-sm">No verification attempts yet.</div>
      )}

      {logPage && logPage.content.length > 0 && (
        <>
          <div className="card overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-ink-faint">
                  <th className="px-5 py-3 font-medium">Credential ID</th>
                  <th className="px-5 py-3 font-medium">Result</th>
                  <th className="px-5 py-3 font-medium">Verifier</th>
                  <th className="px-5 py-3 font-medium">When</th>
                </tr>
              </thead>
              <tbody>
                {logPage.content.map((log) => (
                  <tr key={log.id} className="border-b border-line-soft last:border-0">
                    <td className="px-5 py-3.5 font-mono text-xs text-ink-soft">
                      {log.credentialId ? `${log.credentialId.slice(0, 13)}…` : '\u2014'}
                    </td>
                    <td className="px-5 py-3.5"><StatusBadge status={log.result} /></td>
                    <td className="px-5 py-3.5 text-ink-faint text-xs truncate max-w-xs">{log.verifierInfo}</td>
                    <td className="px-5 py-3.5 text-ink-soft font-mono text-xs">
                      {new Date(log.verifiedAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex items-center justify-between mt-4 text-sm">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={logPage.first}
              className="btn-ghost"
            >
              &larr; Previous
            </button>
            <span className="text-ink-faint text-xs">
              Page {logPage.number + 1} of {Math.max(1, logPage.totalPages)}
            </span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={logPage.last}
              className="btn-ghost"
            >
              Next &rarr;
            </button>
          </div>
        </>
      )}
    </DashboardShell>
  )
}
