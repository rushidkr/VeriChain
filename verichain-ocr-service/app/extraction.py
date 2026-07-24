"""
Extracts a VeriChain credential ID from an uploaded image or PDF of a certificate.

Two strategies, tried in order:
  1. QR decode  - the credential's QR code encodes the full verification URL
                  (e.g. http://localhost:5173/verify/<uuid>). This is the reliable path,
                  since it's exactly what VeriChain itself generates.
  2. OCR fallback - if no QR is detected (a photocopy, a cropped screenshot, a printed
                  certificate that only shows the ID as text), fall back to Tesseract OCR
                  and search the extracted text for a UUID-shaped string.

Kept dependency-light and framework-free so it can be unit tested without spinning up FastAPI.
"""

import io
import os
import re
from dataclasses import dataclass
from typing import Optional

import fitz  # PyMuPDF
import pytesseract
from PIL import Image, ImageOps, ImageFilter
from pyzbar.pyzbar import decode as decode_qr

UUID_PATTERN = re.compile(
    r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
)
LENIENT_UUID_PATTERN = re.compile(
    r"\b([0-9a-zA-Z]{7,9})[-._\s]+([0-9a-zA-Z]{3,5})[-._\s]+([0-9a-zA-Z]{3,5})[-._\s]+([0-9a-zA-Z]{3,5})[-._\s]+([0-9a-zA-Z]{11,13})\b"
)
CONTIGUOUS_UUID_PATTERN = re.compile(
    r"\b([0-9a-zA-Z]{32})\b"
)

PDF_RENDER_DPI = 200
OCR_CONFIGS = [
    "--oem 3 --psm 6",
    "--oem 1 --psm 6",
    "--oem 3 --psm 11",
    "--psm 6 -c tessedit_char_whitelist=0123456789abcdefABCDEF-",
    "--psm 11 -c tessedit_char_whitelist=0123456789abcdefABCDEF-",
]

TESSERACT_PATH = os.getenv("TESSERACT_CMD", "C:/Program Files/Tesseract-OCR/tesseract.exe")
if os.path.exists(TESSERACT_PATH):
    pytesseract.pytesseract.tesseract_cmd = TESSERACT_PATH


@dataclass
class ExtractionResult:
    credential_id: Optional[str]
    method: Optional[str]  # "qr" | "ocr" | None
    raw_qr_payload: Optional[str] = None


def load_images_from_upload(file_bytes: bytes, content_type: str) -> list[Image.Image]:
    """Returns a list of PIL images: one for a plain image upload, one per page for a PDF."""
    if content_type == "application/pdf" or file_bytes[:4] == b"%PDF":
        images = []
        doc = fitz.open(stream=file_bytes, filetype="pdf")
        try:
            zoom = PDF_RENDER_DPI / 72
            matrix = fitz.Matrix(zoom, zoom)
            for page in doc:
                pix = page.get_pixmap(matrix=matrix)
                img = Image.frombytes("RGB", (pix.width, pix.height), pix.samples)
                images.append(img)
        finally:
            doc.close()
        return images

    img = Image.open(io.BytesIO(file_bytes))
    img = ImageOps.exif_transpose(img)
    return [img.convert("RGB")]


def _normalize_char(c: str) -> str:
    c = c.lower()
    mapping = {
        'o': '0',
        'l': '1',
        'i': '1',
        's': '5',
        'z': '2',
    }
    return mapping.get(c, c)


def _extract_uuid_from_text(text: str) -> Optional[str]:
    # 1. Try strict match first
    match = UUID_PATTERN.search(text)
    if match:
        return match.group(0).lower()

    # 2. Try lenient match (blocks separated by hyphens/spaces/dots/underscores)
    lenient_matches = LENIENT_UUID_PATTERN.finditer(text)
    for m in lenient_matches:
        groups = m.groups()
        raw_str = "".join(groups)
        normalized = "".join(_normalize_char(c) for c in raw_str)
        if len(normalized) == 32 and all(c in "0123456789abcdef" for c in normalized):
            reconstructed = f"{normalized[:8]}-{normalized[8:12]}-{normalized[12:16]}-{normalized[16:20]}-{normalized[20:]}"
            return reconstructed

    # 3. Try contiguous match (32 hex-like characters)
    contiguous_matches = CONTIGUOUS_UUID_PATTERN.finditer(text)
    for m in contiguous_matches:
        raw_str = m.group(1)
        normalized = "".join(_normalize_char(c) for c in raw_str)
        if len(normalized) == 32 and all(c in "0123456789abcdef" for c in normalized):
            reconstructed = f"{normalized[:8]}-{normalized[8:12]}-{normalized[12:16]}-{normalized[16:20]}-{normalized[20:]}"
            return reconstructed

    return None


def try_qr_extraction(images: list[Image.Image]) -> Optional[ExtractionResult]:
    for image in images:
        decoded = decode_qr(image)
        for symbol in decoded:
            payload = symbol.data.decode("utf-8", errors="ignore")
            # The QR encodes a full verification URL - the credential ID is the last path segment.
            candidate = payload.rstrip("/").split("/")[-1]
            uuid_match = _extract_uuid_from_text(candidate) or _extract_uuid_from_text(payload)
            if uuid_match:
                return ExtractionResult(credential_id=uuid_match, method="qr", raw_qr_payload=payload)
    return None


def _prepare_image_for_ocr(image: Image.Image) -> Image.Image:
    gray = image.convert("L")
    gray = ImageOps.autocontrast(gray)
    gray = gray.filter(ImageFilter.SHARPEN)
    gray = gray.resize((gray.width * 2, gray.height * 2))
    return gray


def _ocr_text_for_image(image: Image.Image) -> list[str]:
    variants = [image]
    prepared = _prepare_image_for_ocr(image)
    variants.append(prepared)
    variants.append(ImageOps.invert(prepared))

    texts: list[str] = []
    for variant in variants:
        for config in OCR_CONFIGS:
            texts.append(pytesseract.image_to_string(variant, config=config))
    return texts


def try_ocr_extraction(images: list[Image.Image]) -> Optional[ExtractionResult]:
    for image in images:
        for text in _ocr_text_for_image(image):
            uuid_match = _extract_uuid_from_text(text)
            if uuid_match:
                return ExtractionResult(credential_id=uuid_match, method="ocr")
    return None


def extract_credential_id(file_bytes: bytes, content_type: str) -> ExtractionResult:
    images = load_images_from_upload(file_bytes, content_type)

    qr_result = try_qr_extraction(images)
    if qr_result:
        return qr_result

    ocr_result = try_ocr_extraction(images)
    if ocr_result:
        return ocr_result

    return ExtractionResult(credential_id=None, method=None)
