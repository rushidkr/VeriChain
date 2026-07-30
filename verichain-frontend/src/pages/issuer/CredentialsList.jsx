import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import DashboardShell from '../../components/DashboardShell.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import Alert from '../../components/Alert.jsx'
import Spinner from '../../components/Spinner.jsx'
import { ISSUER_NAV } from './nav.js'
import api, { extractErrorMessage } from '../../lib/api'

export default function CredentialsList() {
  const [credentials, setCredentials] = useState([])
  const [pageInfo, setPageInfo] = useState({ totalElements: 0 })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/issuer/credentials', { params: { page: 0, size: 10 } })
      .then(({ data }) => {
        setCredentials(data.content || [])
        setPageInfo({ totalElements: data.totalElements || 0 })
      })
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  return (
    <DashboardShell title="My credentials" subtitle={`${pageInfo.totalElements || credentials.length} issued`} navItems={ISSUER_NAV}>
      {loading && <div className="flex items-center gap-2 text-ink-soft"><Spinner /> Loading…</div>}
      {error && <Alert variant="error">{error}</Alert>}

      {!loading && !error && credentials.length === 0 && (
        <div className="card p-10 text-center">
          <p className="text-ink-soft text-sm mb-4">You haven't issued any credentials yet.</p>
          <Link to="/issuer/issue" className="btn-primary inline-flex">Issue your first credential</Link>
        </div>
      )}

      {credentials.length > 0 && (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-ink-faint">
                <th className="px-5 py-3 font-medium">Holder</th>
                <th className="px-5 py-3 font-medium">Title</th>
                <th className="px-5 py-3 font-medium">Issued</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody>
              {credentials.map((c) => (
                <tr key={c.id} className="border-b border-line-soft last:border-0 hover:bg-paper-dim/50 transition-colors">
                  <td className="px-5 py-3.5">
                    <p className="text-ink font-medium">{c.holderName}</p>
                    <p className="text-xs text-ink-faint">{c.holderEmail}</p>
                  </td>
                  <td className="px-5 py-3.5 text-ink-soft">{c.title}</td>
                  <td className="px-5 py-3.5 text-ink-soft font-mono text-xs">{c.issueDate}</td>
                  <td className="px-5 py-3.5"><StatusBadge status={c.status} /></td>
                  <td className="px-5 py-3.5 text-right">
                    <Link to={`/issuer/credentials/${c.id}`} className="text-bronze text-xs font-medium hover:underline">
                      View &rarr;
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </DashboardShell>
  )
}
