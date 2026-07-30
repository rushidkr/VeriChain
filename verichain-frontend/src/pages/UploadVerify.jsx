import React, { useCallback, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import PublicHeader from '../components/PublicHeader.jsx'
import Alert from '../components/Alert.jsx'
import Spinner from '../components/Spinner.jsx'
import ocrApi, { extractOcrErrorMessage } from '../lib/ocrApi'

const ACCEPTED_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp', 'application/pdf']
const ACCEPTED_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.webp', '.pdf']

export default function UploadVerify() {
  const [file, setFile] = useState(null)
  const [dragActive, setDragActive] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const navigate = useNavigate()

  function pickFile(selected) {
    setError('')
    setNotice('')
    if (!selected) return
    
    const extension = selected.name ? selected.name.slice(selected.name.lastIndexOf('.')).toLowerCase() : ''
    const isAccepted = ACCEPTED_TYPES.includes(selected.type) || ACCEPTED_EXTENSIONS.includes(extension)
    
    if (!isAccepted) {
      setError('Unsupported file type. Upload a PNG, JPEG, WEBP, or PDF.')
      return
    }
    setFile(selected)
  }

  function handleDrop(e) {
    e.preventDefault()
    setDragActive(false)
    pickFile(e.dataTransfer.files?.[0])
  }

  const handleDragOver = useCallback((e) => { e.preventDefault(); setDragActive(true) }, [])
  const handleDragLeave = useCallback(() => setDragActive(false), [])

  async function handleSubmit(e) {
    e.preventDefault()
    if (!file) return
    setLoading(true)
    setError('')
    setNotice('')

    const formData = new FormData()
    formData.append('file', file)

    try {
      const { data } = await ocrApi.post('/ocr/verify', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })

      if (!data.credentialId) {
        setNotice(`${data.message} If the file is a clear scan or screenshot containing the credential ID, try again with better lighting and less blur.`)
        return
      }

      // Reuse the exact same verdict page a scanned QR or pasted ID would land on.
      navigate(`/verify/${data.credentialId}`)
    } catch (err) {
      setError(extractOcrErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <PublicHeader />

      <main className="flex-1 max-w-xl w-full mx-auto px-6 py-16">
        <p className="text-xs uppercase tracking-[0.2em] text-bronze font-medium mb-2 text-center">
          Verify by upload
        </p>
        <h1 className="text-2xl font-display font-semibold text-center mb-2">
          Upload a certificate to verify it
        </h1>
        <p className="text-sm text-ink-soft text-center mb-8">
          We'll scan it for an embedded QR code, or read the credential ID directly off the page.
        </p>

        <form onSubmit={handleSubmit}>
          <label
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            className={`card flex flex-col items-center justify-center gap-2 py-14 px-6 text-center cursor-pointer transition-colors
              ${dragActive ? 'border-bronze bg-bronze-dim' : 'border-dashed'}`}
          >
            <input
              type="file"
              accept={ACCEPTED_TYPES.concat(ACCEPTED_EXTENSIONS).join(',')}
              className="sr-only"
              onChange={(e) => pickFile(e.target.files?.[0])}
            />
            {file ? (
              <>
                <p className="text-sm font-medium text-ink">{file.name}</p>
                <p className="text-xs text-ink-faint">Click or drop to replace</p>
              </>
            ) : (
              <>
                <p className="text-sm font-medium text-ink">Drop a certificate here, or click to browse</p>
                <p className="text-xs text-ink-faint">PNG, JPEG, WEBP, or PDF · up to 10 MB</p>
              </>
            )}
          </label>

          {error && <div className="mt-4"><Alert variant="error">{error}</Alert></div>}
          {notice && <div className="mt-4"><Alert variant="info">{notice}</Alert></div>}

          <button type="submit" disabled={!file || loading} className="btn-bronze w-full mt-5">
            {loading ? <Spinner /> : 'Scan & verify'}
          </button>
        </form>

        <p className="text-center mt-8">
          <Link to="/" className="text-sm text-ink-soft hover:text-ink transition-colors">
            &larr; Or paste a credential ID instead
          </Link>
        </p>
      </main>
    </div>
  )
}
