# VeriChain Frontend

React 18 + Vite + Tailwind frontend for the VeriChain backend.

## Design notes

The recurring visual motif is the **chain ledger** (`ChainLedger.jsx`) — a hash rendered as
connected, colorable segments — used everywhere a hash appears (verify result, credential
detail) so the UI literally shows the mechanism the product is named after, rather than a
generic padlock icon. The verdict on the public verify page is delivered as a **seal stamp**
(`SealStamp.jsx`), a single orchestrated moment rather than scattered micro-animations.

Type: **Fraunces** (display/headers, evokes an official certificate), **Inter** (UI/body),
**IBM Plex Mono** (hashes, credential IDs — reinforces the cryptographic-ledger feel).
Palette: cool archival paper background, deep ink navy, a bronze/seal accent, with distinct
verified/tampered/revoked colors used consistently across badges, the seal, and the chain ledger.

## Setup

```bash
npm install
cp .env.example .env    # point VITE_API_BASE_URL / VITE_OCR_BASE_URL if not on localhost
npm run dev
```

Runs on `http://localhost:5173`. Make sure the backend is running on `http://localhost:8080`
(or update `.env`) — and that the backend's `verichain.public-base-url` matches this frontend's
URL, since that's what gets embedded in generated QR codes.

The `/verify-upload` page additionally calls the OCR microservice (`verichain-ocr-service`,
default `http://localhost:8090`) — start that too if you want the upload-a-certificate flow
working, though the rest of the app functions fine without it.

## Page map

| Route | Access | Purpose |
|---|---|---|
| `/` | public | Landing page + verify-by-ID entry point |
| `/verify/:credentialId` | public | The actual verdict page (what QR codes point to) |
| `/verify-upload` | public | Verify by uploading a photo/scan (calls the OCR microservice) |
| `/login`, `/register` | public | Auth, with a Student/Issuer toggle on register |
| `/issuer` | ISSUER | Profile, approval status, public key |
| `/issuer/issue` | ISSUER | Issue-credential form |
| `/issuer/credentials` | ISSUER | List of issued credentials |
| `/issuer/credentials/:id` | ISSUER | Full chain detail, QR code, revoke |
| `/admin` | ADMIN | Pending issuer approvals |
| `/admin/issuers` | ADMIN | Full issuer roster |
| `/admin/logs` | ADMIN | Paginated verification audit log |

## Structure

```
src/
  components/   Shared UI: ChainLedger, SealStamp, StatusBadge, DashboardShell,
                PublicHeader, Wordmark, Alert, Spinner, ProtectedRoute
  context/      AuthContext (JWT session in localStorage)
  lib/          api.js (backend), ocrApi.js (OCR microservice) — axios instances with
                auth interceptor + 401 handling
  pages/        Landing, VerifyResult, UploadVerify, Login, Register, issuer/*, admin/*
```

## Trying the full flow

1. Register as an issuer at `/register`.
2. Log in as the seeded admin (`admin@verichain.com` / `ChangeMe123!`) and approve the issuer
   from `/admin`.
3. Log back in as the issuer, issue a credential from `/issuer/issue`.
4. Open the credential's verification URL (or scan its QR from the detail page) — it should
   show **Verified**.
5. Edit the credential's data directly in the H2 console, reload the verify page — it should
   flip to **Tampered**.
