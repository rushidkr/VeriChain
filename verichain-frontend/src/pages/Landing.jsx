import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import PublicHeader from '../components/PublicHeader.jsx'
import ChainLedger from '../components/ChainLedger.jsx'
import { useAuth } from '../context/AuthContext.jsx'

const STEPS = [
  {
    n: '01',
    title: 'Paste the credential ID',
    body: 'Every credential has a unique ID, printed alongside its QR code. Scan the code or paste the ID here.',
  },
  {
    n: '02',
    title: 'We recompute the record',
    body: 'The credential\u2019s data is re-hashed and re-linked to the issuer\u2019s chain. Any edit made after issuance breaks this recomputation.',
  },
  {
    n: '03',
    title: 'The signature is checked',
    body: 'The issuer\u2019s digital signature is verified against their public key, confirming who actually issued it.',
  },
]

export default function Landing() {
  const { user } = useAuth()
  const [credentialId, setCredentialId] = useState('')
  const navigate = useNavigate()

  function handleSubmit(e) {
    e.preventDefault()
    const trimmed = credentialId.trim()
    if (!trimmed) return
    // Reuse the /verify/:id page so a pasted ID and a scanned QR land on the exact same flow.
    navigate(`/verify/${encodeURIComponent(trimmed)}`)
  }

  return (
    <div className="min-h-screen flex flex-col">
      <PublicHeader />

      <section className="max-w-3xl mx-auto px-6 pt-20 pb-16 text-center">
        <p className="text-xs uppercase tracking-[0.2em] text-bronze font-medium mb-4">
          Tamper-evident credentials
        </p>
        <h1 className="text-4xl sm:text-5xl font-display font-semibold leading-[1.1] text-ink mb-5">
          Verify any credential<br />in seconds — no phone call required.
        </h1>
        <p className="text-ink-soft text-base max-w-xl mx-auto mb-10">
          Every credential on VeriChain is sealed into a cryptographic hash-chain and signed by
          its issuer. Paste an ID below and we'll tell you, instantly, whether it's genuine.
        </p>

        <form onSubmit={handleSubmit} className="card p-2 flex flex-col sm:flex-row gap-2 max-w-xl mx-auto">
          <input
            type="text"
            value={credentialId}
            onChange={(e) => setCredentialId(e.target.value)}
            placeholder="e.g. 6f2a1c9e-8b3d-4e21-9a10-3f9c2d7b1a44"
            className="flex-1 px-4 py-3 text-sm font-mono rounded-sm focus:outline-none bg-transparent placeholder:text-ink-faint"
          />
          <button type="submit" disabled={!credentialId.trim()} className="btn-bronze px-6">
            Verify
          </button>
        </form>

        <p className="mt-4 text-sm text-ink-faint">
          Don't have the ID? <Link to="/verify-upload" className="text-bronze hover:underline">Upload a photo or scan instead</Link>
        </p>

        {user && (
          <div className="mt-6 flex flex-wrap justify-center gap-3 text-sm">
            {user.role === 'STUDENT' && <Link to="/student" className="btn-ghost">Open student dashboard</Link>}
            {user.role === 'ISSUER' && <Link to="/issuer" className="btn-ghost">Open issuer dashboard</Link>}
            {user.role === 'ADMIN' && <Link to="/admin" className="btn-ghost">Open admin dashboard</Link>}
          </div>
        )}

        <div className="mt-12 flex justify-center opacity-70">
          <ChainLedger hash="a13f9c2d7b1a446f2a1c9e8b3d4e21" state="neutral" segments={10} />
        </div>
      </section>

      <section className="border-t border-line bg-white py-16">
        <div className="max-w-4xl mx-auto px-6">
          <h2 className="text-xl font-display font-semibold text-center mb-10">How verification works</h2>
          <div className="grid sm:grid-cols-3 gap-8">
            {STEPS.map((step) => (
              <div key={step.n}>
                <span className="font-mono text-xs text-bronze">{step.n}</span>
                <h3 className="font-display font-medium text-ink mt-1 mb-1.5">{step.title}</h3>
                <p className="text-sm text-ink-soft leading-relaxed">{step.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="py-16 px-6 text-center">
        <h2 className="text-xl font-display font-semibold text-ink mb-2">Issue credentials your organization can stand behind</h2>
        <p className="text-ink-soft text-sm max-w-md mx-auto mb-6">
          Colleges, companies, and program organizers can register as an issuer to start
          producing verifiable certificates and offer letters.
        </p>
        <div className="flex flex-wrap justify-center gap-3">
          <a href="/register" className="btn-primary inline-flex">Register as an issuer</a>
          <a href="/register" className="btn-ghost inline-flex">Register as a student</a>
        </div>
      </section>

      <footer className="border-t border-line py-8 text-center text-xs text-ink-faint">
        VeriChain &middot; Tamper-evident credential verification
      </footer>
    </div>
  )
}
