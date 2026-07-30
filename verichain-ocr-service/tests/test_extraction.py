import io
import os

import pytest
import qrcode
from PIL import Image, ImageDraw, ImageFont

from app.extraction import extract_credential_id, _extract_uuid_from_text

CRED_ID = "6f2a1c9e-8b3d-4e21-9a10-3f9c2d7b1a44"
FONT_PATHS = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
    "C:/Windows/Fonts/consola.ttf",
    "C:/Windows/Fonts/cour.ttf",
]


def _png_bytes(img: Image.Image) -> bytes:
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def make_qr_certificate() -> bytes:
    url = f"http://localhost:5173/verify/{CRED_ID}"
    qr = qrcode.make(url).resize((300, 300))
    cert = Image.new("RGB", (900, 500), "white")
    cert.paste(qr, (500, 150))
    return _png_bytes(cert)


def make_text_only_certificate() -> bytes:
    font = None
    for path in FONT_PATHS:
        if os.path.exists(path):
            try:
                font = ImageFont.truetype(path, 28)
                break
            except OSError:
                continue

    if font is None:
        font = ImageFont.load_default()

    cert = Image.new("RGB", (1000, 200), "white")
    draw = ImageDraw.Draw(cert)
    draw.text((50, 80), f"Credential ID: {CRED_ID}", fill="black", font=font)
    return _png_bytes(cert)


def make_blank_certificate() -> bytes:
    return _png_bytes(Image.new("RGB", (400, 300), "white"))


def test_uuid_regex_matches_standard_uuid():
    assert _extract_uuid_from_text(f"some text {CRED_ID} more text") == CRED_ID


def test_uuid_regex_returns_none_when_absent():
    assert _extract_uuid_from_text("no uuid in here at all") is None


def test_qr_extraction_takes_priority():
    result = extract_credential_id(make_qr_certificate(), "image/png")
    assert result.method == "qr"
    assert result.credential_id == CRED_ID


def test_ocr_fallback_when_no_qr_present():
    result = extract_credential_id(make_text_only_certificate(), "image/png")
    assert result.method == "ocr"
    assert result.credential_id == CRED_ID


def test_returns_none_when_nothing_found():
    result = extract_credential_id(make_blank_certificate(), "image/png")
    assert result.credential_id is None
    assert result.method is None


def test_uuid_regex_lenient_matching_with_ocr_typos():
    # Misreadings: 'o' -> '0', 'l' -> '1', 's' -> '5', 'z' -> '2'
    typo_text_1 = "some text 6f2a1c9e_8b3d_4e21_9a1o_3f9c2d7b1a44 more text"  # 'o' instead of '0'
    assert _extract_uuid_from_text(typo_text_1) == "6f2a1c9e-8b3d-4e21-9a10-3f9c2d7b1a44"

    typo_text_2 = "ID: 6f2a1c9e.8b3d.4e21.9a10.3f9c2d7b1a44"  # dots instead of hyphens
    assert _extract_uuid_from_text(typo_text_2) == "6f2a1c9e-8b3d-4e21-9a10-3f9c2d7b1a44"

    typo_text_3 = "6f2a1c9e 8b3d 4e21 9a10 3f9c2d7b1a44"  # spaces instead of hyphens
    assert _extract_uuid_from_text(typo_text_3) == "6f2a1c9e-8b3d-4e21-9a10-3f9c2d7b1a44"

    typo_text_4 = "ID: 6f2a1c9e8b3d4e219a103f9c2d7b1a44"  # contiguous 32 chars
    assert _extract_uuid_from_text(typo_text_4) == "6f2a1c9e-8b3d-4e21-9a10-3f9c2d7b1a44"
