import React, { useEffect, useState } from 'react'
import DashboardShell from '../../components/DashboardShell.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import Alert from '../../components/Alert.jsx'
import Spinner from '../../components/Spinner.jsx'
import { ADMIN_NAV } from './nav.js'
import api, { extractErrorMessage } from '../../lib/api'

export default function AllIssuers() {
  const [issuers, setIssuers] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/admin/issuers')
      .then(({ data }) => setIssuers(data))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  return (
    <DashboardShell title="All issuers" subtitle={`${issuers.length} registered`} navItems={ADMIN_NAV}>
      {loading && <div className="flex items-center gap-2 text-ink-soft"><Spinner /> Loading…</div>}
      {error && <Alert variant="error">{error}</Alert>}

      {issuers.length > 0 && (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-ink-faint">
                <th className="px-5 py-3 font-medium">Organization</th>
                <th className="px-5 py-3 font-medium">Contact</th>
                <th className="px-5 py-3 font-medium">Registered</th>
                <th className="px-5 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {issuers.map((issuer) => (
                <tr key={issuer.issuerId} className="border-b border-line-soft last:border-0">
                  <td className="px-5 py-3.5 text-ink font-medium">{issuer.organizationName}</td>
                  <td className="px-5 py-3.5 text-ink-soft">{issuer.contactName} &middot; {issuer.contactEmail}</td>
                  <td className="px-5 py-3.5 text-ink-soft font-mono text-xs">
                    {new Date(issuer.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-5 py-3.5"><StatusBadge status={issuer.approvalStatus} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </DashboardShell>
  )
}
