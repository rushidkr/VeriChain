import React from 'react'

const STYLES = {
  error: 'bg-alert-dim text-alert border-alert/30',
  info: 'bg-bronze-dim text-bronze border-bronze/30',
  success: 'bg-verified-dim text-verified border-verified/30',
}

export default function Alert({ variant = 'info', children }) {
  return (
    <div className={`rounded-sm border px-4 py-3 text-sm ${STYLES[variant]}`}>
      {children}
    </div>
  )
}
