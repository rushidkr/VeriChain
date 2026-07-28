import axios from 'axios'

export const OCR_BASE_URL = import.meta.env.VITE_OCR_BASE_URL || 'http://localhost:8090'

export const ocrApi = axios.create({
  baseURL: OCR_BASE_URL,
})

export function extractOcrErrorMessage(error) {
  return (
    error?.response?.data?.detail ||
    error?.message ||
    'Could not reach the certificate-scanning service.'
  )
}

export default ocrApi
