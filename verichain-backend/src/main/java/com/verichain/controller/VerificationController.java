package com.verichain.controller;

import com.verichain.dto.response.VerificationResponse;
import com.verichain.entity.VerificationResult;
import com.verichain.service.QrCodeService;
import com.verichain.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Fully public API - no auth required. This is the whole point of the product: anyone
 * (a recruiter, another company, a background-check vendor) can verify a credential without
 * ever contacting the issuing institution.
 */
@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;
    private final QrCodeService qrCodeService;

    @GetMapping("/{credentialId}")
    public ResponseEntity<VerificationResponse> verify(@PathVariable String credentialId,
                                                         HttpServletRequest request) {
        String verifierInfo = request.getRemoteAddr() + " | " +
                (request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "unknown");

        // This is the field a human pastes free text into - a mistyped or garbage ID is an
        // expected input, not an error condition. Treat it the same as "no such credential"
        // rather than letting UUID.fromString's IllegalArgumentException surface as a 500.
        UUID parsedId;
        try {
            parsedId = UUID.fromString(credentialId.trim());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(VerificationResponse.builder()
                    .result(VerificationResult.NOT_FOUND)
                    .message("That doesn't look like a valid credential ID. Double-check it and try again.")
                    .chainIntact(false)
                    .signatureValid(false)
                    .build());
        }

        return ResponseEntity.ok(verificationService.verify(parsedId, verifierInfo));
    }

    @GetMapping(value = "/{credentialId}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(@PathVariable String credentialId) {
        UUID parsedId;
        try {
            parsedId = UUID.fromString(credentialId.trim());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        byte[] png = qrCodeService.generateQrCodePng(parsedId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
                .body(png);
    }

    @GetMapping(value = "/{credentialId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getCertificatePdf(@PathVariable String credentialId) {
        UUID parsedId;
        try {
            parsedId = UUID.fromString(credentialId.trim());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        byte[] pdfBytes = verificationService.generateCertificatePdf(parsedId);
        if (pdfBytes == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("certificate-" + parsedId + ".pdf")
                .build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
