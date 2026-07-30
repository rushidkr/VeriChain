import React from 'react'

const STATE_STYLES = {
  neutral: { block: 'bg-white border-line text-ink-soft', link: 'bg-line' },
  loading: { block: 'bg-white border-line text-ink-faint animate-pulse', link: 'bg-line' },
  verified: { block: 'bg-verified-dim border-verified/40 text-verified', link: 'bg-verified/50' },
  tampered: { block: 'bg-alert-dim border-alert/40 text-alert', link: 'bg-alert/50' },
  revoked: { block: 'bg-revoked-dim border-revoked/40 text-revoked', link: 'bg-revoked/50' },
  expired: { block: 'bg-paper-dim border-bronze/40 text-bronze', link: 'bg-bronze/30' },
}

/**
 * Splits a hex hash into fragments and renders them as connected blocks, like links in a chain.
 * This is the app's recurring visual motif: the literal hash-chain made visible, rather than an
 * abstract padlock/shield icon. State drives the color: neutral while unverified, verified/
 * tampered/revoked once a check has run.
 */
export default function ChainLedger({ hash, state = 'neutral', segments = 8, compact = false }) {
  const safeHash = hash || ''.padEnd(segments * 4, '0')
  const chunkSize = Math.ceil(safeHash.length / segments) || 1
  const chunks = []
  for (let i = 0; i < safeHash.length; i += chunkSize) {
    chunks.push(safeHash.slice(i, i + chunkSize))
  }

  const styles = STATE_STYLES[state] || STATE_STYLES.neutral

  return (
    <div className="flex items-center flex-wrap gap-0" role="img" aria-label={`Hash chain: ${safeHash}`}>
      {chunks.map((chunk, i) => (
        <React.Fragment key={i}>
          {i > 0 && (
            <span
              className={`inline-block h-px w-2 sm:w-3 ${styles.link}`}
              style={{ animationDelay: `${i * 35}ms` }}
            />
          )}
          <span
            className={`font-mono border rounded-sm ${compact ? 'text-[10px] px-1 py-0.5' : 'text-xs px-1.5 py-1'} ${styles.block} animate-linkLight`}
            style={{ animationDelay: `${i * 35}ms` }}
          >
            {chunk}
          </span>
        </React.Fragment>
      ))}
    </div>
  )
}
