# VeriChain 🛡️🔗

VeriChain is a secure, decentralized credential issuance and verification ecosystem. It allows educational institutions and organizations to issue tamper-proof certificates, which can be verified instantly by third parties (like recruiters) using cryptographic signatures, hash chaining, and OCR-based document parsing.

## 🏗️ Architecture Overview

The system consists of three primary components:

1. **`verichain-backend` (Spring Boot 3.3.2):**
   * Manages user profiles (Issuers, Students, Admins).
   * Handles credential lifecycle (Issuance, Revocation).
   * Cryptographically links credentials using **Hash Chaining** (similar to a blockchain ledger).
   * Generates public verification QR codes.
   * Generates downloadable physical landscape certificate PDFs (powered by OpenPDF).
   * Connects to a persistent local **MySQL Database**.

2. **`verichain-ocr-service` (FastAPI + Tesseract OCR):**
   * Decodes verification QR codes directly from uploaded certificate scans/photos.
   * Implements fallback **OCR engine** to extract raw credential IDs if the QR code is damaged or unreadable.
   * Includes advanced error handling (EXIF transposing for auto-rotation and lenient UUID normalization to correct common OCR typos).

3. **`verichain-frontend` (React + Vite + Tailwind CSS):**
   * **Issuer Portal:** Dashboard to issue credentials, view history, and revoke/re-issue certificates.
   * **Student Portal:** Personal inbox to view and download physical certificate PDFs.
   * **Verifier Portal:** Publicly accessible upload/verification page to check the authenticity of certificates.

---

## 🛠️ Tech Stack

* **Backend:** Java 17, Spring Boot, Spring Security (JWT), Hibernate/JPA, MySQL, OpenPDF, ZXing (QR codes)
* **OCR Microservice:** Python 3.10+, FastAPI, PyTesseract, OpenCV, ZBar
* **Frontend:** Javascript, React 18, Vite, Tailwind CSS, Axios

---

## 🚀 Setup & Installation

### 1. Database Configuration
* Make sure a local MySQL instance is running on port `3306`.
* Configure your username and password in [application.properties](verichain-backend/src/main/resources/application.properties):
  ```properties
  spring.datasource.username=your_username
  spring.datasource.password=your_password
  ```

### 2. Run the Backend (Spring Boot)
```bash
cd verichain-backend
mvn spring-boot:run
```

### 3. Run the OCR Microservice (FastAPI)
```bash
cd verichain-ocr-service
python -m venv .venv
# Activate virtual environment:
# Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

### 4. Run the Frontend (React)
```bash
cd verichain-frontend
npm install
npm run dev
```

---

## 🔒 Security & Verification Flow

1. **Hash Chaining:** Each credential contains its data hash and cryptographically references the previous credential's hash, forming an immutable chain of records.
2. **Digital Signatures:** Issuers sign the final chain hash with their private key, which recruiters can verify against the issuer's public key.
3. **Double Verification:** Scanned certificates are validated either via embedded QR code data or raw text ID extraction using OCR.
