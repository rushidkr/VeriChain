package com.verichain.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.verichain.dto.response.VerificationResponse;
import com.verichain.entity.Credential;
import com.verichain.entity.CredentialStatus;
import com.verichain.entity.VerificationLog;
import com.verichain.entity.VerificationResult;
import com.verichain.repository.CredentialRepository;
import com.verichain.repository.VerificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final CredentialRepository credentialRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final HashChainService hashChainService;
    private final SignatureService signatureService;
    private final QrCodeService qrCodeService;

    /**
     * The core verification flow, run against a credential ID scanned from a QR
     * code or pasted by a recruiter. Three independent checks are performed: 1.
     * Content integrity - recomputed dataHash matches the stored dataHash 2.
     * Chain integrity - recomputed chainHash (data + previousChainHash) matches
     * stored chainHash 3. Authenticity - the issuer's signature over chainHash
     * verifies with their public key ANY failure among these means the
     * credential cannot be trusted, regardless of how convincing the displayed
     * fields look.
     */
    @Transactional
    public VerificationResponse verify(UUID credentialId, String verifierInfo) {
        Optional<Credential> maybeCredential = credentialRepository.findById(credentialId);

        if (maybeCredential.isEmpty()) {
            log(null, VerificationResult.NOT_FOUND, verifierInfo);
            return VerificationResponse.builder()
                    .result(VerificationResult.NOT_FOUND)
                    .message("No credential exists with this ID. It may be forged or mistyped.")
                    .chainIntact(false)
                    .signatureValid(false)
                    .build();
        }

        Credential credential = maybeCredential.get();

        HashChainService.CredentialData data = new HashChainService.CredentialData(
                credential.getHolderName(),
                credential.getHolderEmail(),
                credential.getCredentialType().name(),
                credential.getTitle(),
                credential.getDescription(),
                credential.getIssueDate(),
                credential.getExpiryDate(),
                credential.getIssuer().getId()
        );

        boolean chainIntact = hashChainService.isChainIntact(
                data,
                credential.getPreviousChainHash(),
                credential.getDataHash(),
                credential.getChainHash()
        );

        boolean signatureValid = signatureService.verify(
                credential.getChainHash(),
                credential.getSignature(),
                credential.getIssuer().getPublicKey()
        );

        VerificationResult result;
        String message;

        if (!chainIntact || !signatureValid) {
            result = VerificationResult.TAMPERED;
            message = "This credential's data does not match its cryptographic record. "
                    + "It may have been altered after issuance, or is not authentic.";
        } else if (credential.getStatus() == CredentialStatus.REVOKED) {
            result = VerificationResult.REVOKED;
            message = "This credential was valid at issuance but has since been revoked by the issuer.";
        } else if (credential.getExpiryDate() != null && LocalDate.now().isAfter(credential.getExpiryDate())) {
            result = VerificationResult.EXPIRED;
            message = "This credential has expired and is no longer valid for use.";
        } else {
            result = VerificationResult.VALID;
            message = "This credential is authentic and has not been altered since issuance.";
        }

        log(credentialId, result, verifierInfo);

        return VerificationResponse.builder()
                .result(result)
                .message(message)
                .credentialId(credential.getId())
                .issuerOrganization(credential.getIssuer().getOrganizationName())
                .holderName(credential.getHolderName())
                .credentialType(credential.getCredentialType())
                .title(credential.getTitle())
                .issueDate(credential.getIssueDate())
                .expiryDate(credential.getExpiryDate())
                .chainIntact(chainIntact)
                .signatureValid(signatureValid)
                .build();
    }

    private void log(UUID credentialId, VerificationResult result, String verifierInfo) {
        VerificationLog logEntry = VerificationLog.builder()
                .credentialId(credentialId)
                .result(result)
                .verifierInfo(verifierInfo)
                .build();
        verificationLogRepository.save(logEntry);
    }

    @Transactional(readOnly = true)
    public byte[] generateCertificatePdf(UUID credentialId) {
        Optional<Credential> maybeCredential = credentialRepository.findById(credentialId);
        if (maybeCredential.isEmpty()) {
            return null;
        }
        Credential credential = maybeCredential.get();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Draw borders
            PdfContentByte canvas = writer.getDirectContent();
            canvas.setColorStroke(new java.awt.Color(197, 168, 128)); // #C5A880 (Bronze)
            canvas.setLineWidth(4);
            canvas.rectangle(20, 20, PageSize.A4.rotate().getWidth() - 40, PageSize.A4.rotate().getHeight() - 40);
            canvas.stroke();

            canvas.setLineWidth(1);
            canvas.rectangle(26, 26, PageSize.A4.rotate().getWidth() - 52, PageSize.A4.rotate().getHeight() - 52);
            canvas.stroke();

            // Font styles
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, new java.awt.Color(44, 44, 44));
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 16, new java.awt.Color(102, 102, 102));
            Font boldBodyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new java.awt.Color(28, 28, 28));
            Font studentFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, new java.awt.Color(28, 28, 28));
            Font monoFont = FontFactory.getFont(FontFactory.COURIER, 12, new java.awt.Color(74, 74, 74));
            Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new java.awt.Color(120, 120, 120));

            // Content paragraphs
            Paragraph spacer = new Paragraph("\n");
            document.add(spacer);

            // Title
            Paragraph title = new Paragraph("VeriChain Certificate of Authenticity", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Subtitle
            Paragraph certSubtitle = new Paragraph("This is to certify that", bodyFont);
            certSubtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(certSubtitle);
            document.add(new Paragraph("\n"));

            // Student Name
            Paragraph studentName = new Paragraph(credential.getHolderName(), studentFont);
            studentName.setAlignment(Element.ALIGN_CENTER);
            document.add(studentName);
            document.add(new Paragraph("\n"));

            // Body text
            Paragraph courseInfo = new Paragraph("has successfully been awarded the credential:", bodyFont);
            courseInfo.setAlignment(Element.ALIGN_CENTER);
            document.add(courseInfo);

            Paragraph courseTitle = new Paragraph(credential.getTitle(), boldBodyFont);
            courseTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(courseTitle);
            document.add(new Paragraph("\n"));

            // Institute Info
            Paragraph institute = new Paragraph("Issued by: " + credential.getIssuer().getOrganizationName(), boldBodyFont);
            institute.setAlignment(Element.ALIGN_CENTER);
            document.add(institute);
            
            Paragraph issueDate = new Paragraph("Issue Date: " + credential.getIssueDate(), infoFont);
            issueDate.setAlignment(Element.ALIGN_CENTER);
            document.add(issueDate);
            document.add(new Paragraph("\n"));

            // Add Credential ID
            Paragraph credIdPara = new Paragraph("Credential ID: " + credential.getId().toString(), monoFont);
            credIdPara.setAlignment(Element.ALIGN_CENTER);
            document.add(credIdPara);
            document.add(new Paragraph("\n"));

            // Load QR Code and attach
            byte[] qrBytes = qrCodeService.generateQrCodePng(credential.getId());
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.scaleAbsolute(100, 100);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            document.add(qrImage);

            // Scan to verify caption
            Paragraph verifyCaption = new Paragraph("Scan QR code to verify authenticity.", infoFont);
            verifyCaption.setAlignment(Element.ALIGN_CENTER);
            document.add(verifyCaption);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF", e);
        }
    }
}
