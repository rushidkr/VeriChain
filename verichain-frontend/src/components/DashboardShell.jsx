import React from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import Wordmark from './Wordmark.jsx'
import { useAuth } from '../context/AuthContext.jsx'

export default function DashboardShell({ title, subtitle, navItems, children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <div className="min-h-screen flex bg-paper">
      <aside className="w-60 shrink-0 bg-ink text-paper flex flex-col">
        <div className="h-16 flex items-center px-6 border-b border-white/10">
          <Wordmark variant="light" />
        </div>

        <nav className="flex-1 px-3 py-6 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `block px-3 py-2.5 rounded-sm text-sm transition-colors ${
                  isActive ? 'bg-white/10 text-paper font-medium' : 'text-white/65 hover:bg-white/5 hover:text-paper'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="px-6 py-5 border-t border-white/10">
          <p className="text-sm font-medium text-paper truncate">{user?.name}</p>
          <p className="text-xs text-white/50 truncate mb-3">{user?.email}</p>
          <button onClick={handleLogout} className="text-xs text-bronze-soft hover:text-paper transition-colors">
            Log out
          </button>
        </div>
      </aside>

      <main className="flex-1 min-w-0">
        <div className="border-b border-line bg-white px-8 py-6">
          <h1 className="text-2xl font-display font-semibold text-ink">{title}</h1>
          {subtitle && <p className="text-sm text-ink-soft mt-1">{subtitle}</p>}
        </div>
        <div className="p-8 max-w-5xl">{children}</div>
      </main>
    </div>
  )
}
