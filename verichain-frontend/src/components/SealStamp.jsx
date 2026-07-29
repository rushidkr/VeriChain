import React from 'react'

const VARIANTS = {
  VALID: {
    ring: 'border-verified text-verified bg-verified-dim',
    label: 'Verified',
    sub: 'Authentic & unaltered',
  },
  TAMPERED: {
    ring: 'border-alert text-alert bg-alert-dim',
    label: 'Tampered',
    sub: 'Does not match issued record',
  },
  REVOKED: {
    ring: 'border-revoked text-revoked bg-revoked-dim',
    label: 'Revoked',
    sub: 'Withdrawn by issuer',
  },
  EXPIRED: {
    ring: 'border-bronze text-bronze bg-paper-dim',
    label: 'Expired',
    sub: 'Validity window has passed',
  },
  NOT_FOUND: {
    ring: 'border-ink-faint text-ink-faint bg-paper-dim',
    label: 'Not found',
    sub: 'No matching record',
  },
}

export default function SealStamp({ result }) {
  const variant = VARIANTS[result] || VARIANTS.NOT_FOUND

  return (
    <div className="flex flex-col items-center gap-3 animate-stampIn">
      <div
        className={`w-32 h-32 rounded-full border-[3px] flex flex-col items-center justify-center
                    ${variant.ring} -rotate-3 select-none`}
        style={{ borderStyle: 'double', borderWidth: '6px' }}
      >
        <span className="font-display font-semibold uppercase tracking-wide text-base leading-tight text-center px-2">
          {variant.label}
        </span>
      </div>
      <p className="text-sm text-ink-soft">{variant.sub}</p>
    </div>
  )
}
