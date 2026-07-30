import React from 'react'
import { Link } from 'react-router-dom'
import Wordmark from './Wordmark.jsx'
import { useAuth } from '../context/AuthContext.jsx'

export default function PublicHeader() {
  const { user } = useAuth()

  const dashboardPath = user?.role === 'ADMIN' ? '/admin' : user?.role === 'ISSUER' ? '/issuer' : user?.role === 'STUDENT' ? '/student' : null

  return (
    <header className="border-b border-line bg-paper/90 backdrop-blur sticky top-0 z-10">
      <div className="max-w-5xl mx-auto px-6 h-16 flex items-center justify-between">
        <Wordmark />
        <nav className="flex items-center gap-6 text-sm">
          <Link to="/" className="text-ink-soft hover:text-ink transition-colors">Verify a credential</Link>
          {dashboardPath ? (
            <Link to={dashboardPath} className="btn-primary !py-2">Dashboard</Link>
          ) : (
            <>
              <Link to="/login" className="text-ink-soft hover:text-ink transition-colors">Log in</Link>
              <Link to="/register" className="btn-primary !py-2">Get started</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
