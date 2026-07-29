import React from 'react'
import { Link } from 'react-router-dom'

export default function Wordmark({ to = '/', variant = 'dark' }) {
  const isLight = variant === 'light'
  return (
    <Link to={to} className="inline-flex items-center gap-2 group">
      <span className={`w-7 h-7 rounded-sm flex items-center justify-center ${isLight ? 'bg-paper' : 'bg-ink'}`}>
        <span className="w-2.5 h-2.5 rounded-full border-2 border-bronze-soft" />
      </span>
      <span className={`font-display text-lg font-semibold tracking-tight transition-colors ${
        isLight ? 'text-paper' : 'text-ink group-hover:text-ink-soft'
      }`}>
        VeriChain
      </span>
    </Link>
  )
}
