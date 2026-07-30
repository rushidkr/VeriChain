import io
from unittest.mock import patch, AsyncMock

from fastapi.testclient import TestClient
from PIL import Image
import qrcode

from app.main import app

client = TestClient(app)

CRED_ID = "6f2a1c9e-8b3d-4e21-9a10-3f9c2d7b1a44"


def make_qr_certificate_bytes() -> bytes:
    url = f"http://localhost:5173/verify/{CRED_ID}"
    qr = qrcode.make(url).resize((300, 300))
    cert = Image.new("RGB", (900, 500), "white")
    cert.paste(qr, (500, 150))
    buf = io.BytesIO()
    cert.save(buf, format="PNG")
    return buf.getvalue()


def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_rejects_unsupported_file_type():
    response = client.post(
        "/ocr/verify",
        files={"file": ("cert.txt", b"not an image", "text/plain")},
    )
    assert response.status_code == 415


def test_rejects_empty_file():
    response = client.post(
        "/ocr/verify",
        files={"file": ("cert.png", b"", "image/png")},
    )
    assert response.status_code == 400


@patch("app.main.verify_credential", new_callable=AsyncMock)
def test_successful_qr_extraction_and_verification(mock_verify):
    mock_verify.return_value = {
        "result": "VALID",
        "message": "This credential is authentic and has not been altered since issuance.",
        "credentialId": CRED_ID,
        "holderName": "Rushi Patil",
    }

    response = client.post(
        "/ocr/verify",
        files={"file": ("cert.png", make_qr_certificate_bytes(), "image/png")},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["extractionMethod"] == "qr"
    assert body["credentialId"] == CRED_ID
    assert body["verification"]["result"] == "VALID"
    mock_verify.assert_awaited_once_with(CRED_ID)


def test_no_credential_found_returns_helpful_message():
    blank = Image.new("RGB", (300, 200), "white")
    buf = io.BytesIO()
    blank.save(buf, format="PNG")

    response = client.post(
        "/ocr/verify",
        files={"file": ("cert.png", buf.getvalue(), "image/png")},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["credentialId"] is None
    assert "Couldn't find" in body["message"]
