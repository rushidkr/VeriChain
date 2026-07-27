package com.verichain.service;

import com.verichain.entity.Credential;
import com.verichain.entity.CredentialStatus;
import com.verichain.entity.CredentialType;
import com.verichain.entity.IssuerProfile;
import com.verichain.entity.VerificationResult;
import com.verichain.repository.CredentialRepository;
import com.verichain.repository.VerificationLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private VerificationLogRepository verificationLogRepository;

    @Mock
    private HashChainService hashChainService;

    @Mock
    private SignatureService signatureService;

    @Mock
    private QrCodeService qrCodeService;

    @InjectMocks
    private VerificationService verificationService;

    @Test
    void verifyReturnsExpiredWhenCredentialExpiryDateHasPassed() {
        UUID credentialId = UUID.randomUUID();
        Credential credential = Credential.builder()
                .id(credentialId)
                .issuer(IssuerProfile.builder().id(99L).organizationName("Acme University").publicKey("public-key").build())
                .holderName("Ada Lovelace")
                .holderEmail("ada@example.com")
                .credentialType(CredentialType.DEGREE)
                .title("Computer Science Degree")
                .description("Completed")
                .issueDate(LocalDate.of(2024, 1, 1))
                .expiryDate(LocalDate.of(2024, 6, 1))
                .dataHash("hash")
                .previousChainHash("prev")
                .chainHash("chain")
                .signature("signature")
                .status(CredentialStatus.ACTIVE)
                .build();

        when(credentialRepository.findById(credentialId)).thenReturn(Optional.of(credential));
        when(hashChainService.isChainIntact(any(), any(), any(), any())).thenReturn(true);
        when(signatureService.verify(any(), any(), any())).thenReturn(true);
        when(verificationLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = verificationService.verify(credentialId, "127.0.0.1");

        assertThat(response.getResult()).isEqualTo(VerificationResult.EXPIRED);
        assertThat(response.getMessage()).contains("expired");
    }

    @Test
    void generateCertificatePdfReturnsPdfBytes() {
        UUID credentialId = UUID.randomUUID();
        Credential credential = Credential.builder()
                .id(credentialId)
                .issuer(IssuerProfile.builder().id(99L).organizationName("Acme University").publicKey("public-key").build())
                .holderName("Ada Lovelace")
                .holderEmail("ada@example.com")
                .credentialType(CredentialType.DEGREE)
                .title("Computer Science Degree")
                .description("Completed")
                .issueDate(LocalDate.of(2024, 1, 1))
                .dataHash("hash")
                .previousChainHash("prev")
                .chainHash("chain")
                .signature("signature")
                .status(CredentialStatus.ACTIVE)
                .build();

        when(credentialRepository.findById(credentialId)).thenReturn(Optional.of(credential));
        byte[] mockQrBytes = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=");
        when(qrCodeService.generateQrCodePng(credentialId)).thenReturn(mockQrBytes);

        byte[] pdfBytes = verificationService.generateCertificatePdf(credentialId);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }
}
