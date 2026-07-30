# VeriChain OCR Verification Service (Phase 8)

A small Python/FastAPI microservice that lets someone verify a credential from a **photo or
scanned PDF of the certificate itself** — no need to type or paste the ID. This is the
Java+Python polyglot piece of the project: Spring Boot handles the core system, this service
handles the computer-vision side, and they talk to each other over plain HTTP.

## How it works

```
Upload (image or PDF)
        │
        ▼
1. QR decode (pyzbar)  ──── found? ──► credential ID extracted
        │ not found
        ▼
2. OCR fallback (Tesseract) ─ found? ─► credential ID extracted (regex search for a UUID)
        │ not found
        ▼
   "Couldn't find a credential ID" response
        │
        ▼ (ID found via either path)
3. Call Spring Boot's public GET /api/verify/{id}
        │
        ▼
   Same VALID / TAMPERED / REVOKED / NOT_FOUND verdict as pasting the ID directly
```

QR decode is tried first because it's exactly what VeriChain's own QR codes encode (the full
verification URL) — reliable and unambiguous. OCR is a fallback for photocopies, cropped
screenshots, or certificates where only the ID is printed as text, not a scannable code.

## This one is actually tested

Unlike the Java/React code (which I couldn't compile in this sandbox — no Maven/npm registry
access), this sandbox *does* have PyPI and apt access, so I installed the real dependencies
(`tesseract-ocr`, `libzbar0`, FastAPI, pyzbar, pytesseract, PyMuPDF) and ran it for real:

- **10/10 automated tests pass** (`tests/test_extraction.py`, `tests/test_main.py`) — QR
  priority, OCR fallback, unsupported file types, empty files, no-match handling.
- **Live end-to-end round trip confirmed** with `uvicorn` actually running and a mock backend
  standing in for Spring Boot: uploaded a synthetic certificate PNG with an embedded QR →
  got back `"extractionMethod": "qr"` and a `VALID` verdict. Repeated with a QR-less
  text-only certificate → correctly fell back to OCR. Repeated with a one-page PDF → correctly
  rendered the page and QR-decoded it.

Run the tests yourself:
```bash
pip install -r requirements.txt
pytest tests/ -v
```

## Running it

**Locally:**
```bash
# Windows
# Install Tesseract OCR and make sure the binary is available at:
# C:\Program Files\Tesseract-OCR\tesseract.exe

# Optional: set this in your shell before starting the service
# set TESSERACT_CMD=C:\Program Files\Tesseract-OCR\tesseract.exe

pip install -r requirements.txt
set VERICHAIN_API_BASE_URL=http://localhost:8080   # your Spring Boot backend
uvicorn app.main:app --reload --port 8090
```

**With Docker** (bundles tesseract/zbar for you):
```bash
docker build -t verichain-ocr .
docker run -p 8090:8090 -e VERICHAIN_API_BASE_URL=http://host.docker.internal:8080 verichain-ocr
```

## API

`POST /ocr/verify` — multipart form upload, field name `file` (PNG/JPEG/WEBP/PDF, max 10 MB)

```bash
curl -X POST http://localhost:8090/ocr/verify \
  -F "file=@certificate.png;type=image/png"
```

Response:
```json
{
  "extractionMethod": "qr",
  "credentialId": "6f2a1c9e-8b3d-4e21-9a10-3f9c2d7b1a44",
  "verification": {
    "result": "VALID",
    "message": "This credential is authentic and has not been altered since issuance.",
    "...": "...same shape as GET /api/verify/{id} on the backend"
  },
  "message": "Credential ID extracted via QR and verified."
}
```

If no ID could be extracted, `credentialId`/`verification` are `null` with an explanatory
`message`. If the backend can't be reached, returns `503`.

`GET /health` — liveness check, returns `{"status": "ok"}`.

## Structure

```
app/
  main.py              FastAPI app, the /ocr/verify endpoint
  extraction.py        QR decode + OCR fallback logic (framework-free, unit-testable alone)
  verichain_client.py  Thin async HTTP client that calls the Spring Boot backend
tests/
  test_extraction.py   5 tests against extraction.py directly
  test_main.py         5 tests against the FastAPI endpoint (backend call mocked)
requirements.txt
Dockerfile
```

## Wired into the frontend

The React app's `/verify-upload` page (`src/pages/UploadVerify.jsx`) calls this service directly
via `VITE_OCR_BASE_URL` (default `http://localhost:8090`), then navigates to the same
`/verify/:id` result page a pasted ID or scanned QR would land on. Entry points: a link under
the main paste-ID form on the landing page, and a suggestion on the verify page when an ID
isn't found (maybe they only have the physical certificate).
