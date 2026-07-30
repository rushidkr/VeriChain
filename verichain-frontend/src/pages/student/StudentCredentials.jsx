import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import DashboardShell from '../../components/DashboardShell.jsx'
import Spinner from '../../components/Spinner.jsx'
import Alert from '../../components/Alert.jsx'
import StatusBadge from '../../components/StatusBadge.jsx'
import api, { API_BASE_URL, extractErrorMessage } from '../../lib/api'

const STUDENT_NAV = [
  { to: '/student', label: 'My credentials', end: true },
]

export default function StudentCredentials() {
  const [credentials, setCredentials] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get('/api/student/credentials', { params: { page: 0, size: 10 } })
      .then(({ data }) => setCredentials(data.content || []))
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [])

  return (
    <DashboardShell title="My credentials" subtitle="Credentials issued to your email" navItems={STUDENT_NAV}>
      {loading && <div className="flex items-center gap-2 text-ink-soft"><Spinner /> Loading…</div>}
      {error && <Alert variant="error">{error}</Alert>}

      {!loading && !error && (
        <div className="card p-4 mb-6 border-line bg-paper-dim flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="font-medium text-ink">Your credential inbox</p>
            <p className="text-sm text-ink-soft">Any credential issued to your email address appears here automatically.</p>
          </div>
          <div className="flex gap-2">
            <Link to="/" className="btn-ghost">Verify another credential</Link>
            <Link to="/verify-upload" className="btn-primary">Upload a credential</Link>
          </div>
        </div>
      )}

      {!loading && !error && credentials.length === 0 && (
        <div className="card p-10 text-center">
          <p className="text-ink-soft text-sm">No credentials have been issued to your email yet.</p>
        </div>
      )}

      {credentials.length > 0 && (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-ink-faint">
                <th className="px-5 py-3 font-medium">Issuer</th>
                <th className="px-5 py-3 font-medium">Title</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 font-medium">Issued</th>
                <th className="px-5 py-3 font-medium">Action</th>
              </tr>
            </thead>
            <tbody>
              {credentials.map((c) => (
                <tr key={c.id} className="border-b border-line-soft last:border-0">
                  <td className="px-5 py-3.5 text-ink">{c.issuerOrganization}</td>
                  <td className="px-5 py-3.5 text-ink-soft">{c.title}</td>
                  <td className="px-5 py-3.5"><StatusBadge status={c.status} /></td>
                  <td className="px-5 py-3.5 text-ink-soft font-mono text-xs">{c.issueDate}</td>
                  <td className="px-5 py-3.5">
                    <a
                      href={`${API_BASE_URL}/api/verify/${c.id}/pdf`}
                      download={`certificate-${c.id}.pdf`}
                      className="text-bronze hover:underline font-medium"
                    >
                      Download PDF
                    </a>
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
