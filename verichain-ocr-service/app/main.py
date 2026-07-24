from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import httpx

from app.extraction import extract_credential_id
from app.verichain_client import verify_credential

app = FastAPI(
    title="VeriChain OCR Verification Service",
    description=(
        "Accepts a photo or scanned PDF of a certificate, extracts its embedded credential ID "
        "(via QR decode, falling back to OCR text search), and returns the same verification "
        "verdict as pasting the ID directly."
    ),
    version="1.0.0",
)

# Wide open by design - this mirrors the backend's public /api/verify endpoint. Anyone should be
# able to check a certificate without an account, from any origin (a browser extension, a
# recruiter's tool, etc).
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

ALLOWED_CONTENT_TYPES = {"image/png", "image/jpeg", "image/jpg", "image/webp", "application/pdf"}
MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024  # 10 MB, matches the Spring Boot multipart limit


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/ocr/verify")
async def verify_uploaded_certificate(file: UploadFile = File(...)):
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=415,
            detail=f"Unsupported file type '{file.content_type}'. Upload a PNG, JPEG, WEBP, or PDF.",
        )

    file_bytes = await file.read()
    if len(file_bytes) > MAX_FILE_SIZE_BYTES:
        raise HTTPException(status_code=413, detail="File too large - 10 MB maximum.")
    if not file_bytes:
        raise HTTPException(status_code=400, detail="Uploaded file is empty.")

    try:
        extraction = extract_credential_id(file_bytes, file.content_type)
    except Exception as exc:
        raise HTTPException(status_code=422, detail=f"Could not process the file: {exc}") from exc

    if not extraction.credential_id:
        return {
            "extractionMethod": None,
            "credentialId": None,
            "verification": None,
            "message": (
                "Couldn't find a credential ID in this file - no readable QR code and no "
                "UUID-shaped text found. Try a clearer photo, or paste the ID directly."
            ),
        }

    try:
        verification = await verify_credential(extraction.credential_id)
    except httpx.HTTPStatusError as exc:
        raise HTTPException(
            status_code=502,
            detail=f"VeriChain backend rejected the verification request: {exc.response.status_code}",
        ) from exc
    except httpx.RequestError as exc:
        raise HTTPException(
            status_code=503,
            detail="Could not reach the VeriChain backend to verify this credential.",
        ) from exc

    return {
        "extractionMethod": extraction.method,
        "credentialId": extraction.credential_id,
        "verification": verification,
        "message": f"Credential ID extracted via {extraction.method.upper()} and verified.",
    }
