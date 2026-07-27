package com.verichain.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Service
public class QrCodeService {

    @Value("${verichain.public-base-url:http://localhost:5173}")
    private String publicBaseUrl;

    private static final int QR_SIZE = 300;

    public String buildVerificationUrl(UUID credentialId) {
        return publicBaseUrl + "/verify/" + credentialId;
    }

    /** Returns a PNG QR code as a base64 data-URI, ready to drop straight into an <img src="..."> tag. */
    public String generateQrCodeDataUri(UUID credentialId) {
        String url = buildVerificationUrl(credentialId);
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }

    /** Raw PNG bytes, for the dedicated /qrcode endpoint that returns an actual image response. */
    public byte[] generateQrCodePng(UUID credentialId) {
        String url = buildVerificationUrl(credentialId);
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }
}
