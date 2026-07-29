import React from 'react'

const STYLES = {
  ACTIVE: 'bg-verified-dim text-verified',
  VALID: 'bg-verified-dim text-verified',
  APPROVED: 'bg-verified-dim text-verified',
  REVOKED: 'bg-revoked-dim text-revoked',
  TAMPERED: 'bg-alert-dim text-alert',
  REJECTED: 'bg-alert-dim text-alert',
  PENDING: 'bg-bronze-dim text-bronze',
  NOT_FOUND: 'bg-paper-dim text-ink-faint',
}

export default function StatusBadge({ status }) {
  const style = STYLES[status] || 'bg-paper-dim text-ink-faint'
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-medium uppercase tracking-wide ${style}`}>
      {status?.replace('_', ' ')}
    </span>
  )
}
