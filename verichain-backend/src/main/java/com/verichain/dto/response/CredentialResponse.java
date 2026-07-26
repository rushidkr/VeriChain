package com.verichain.dto.response;

import com.verichain.entity.CredentialStatus;
import com.verichain.entity.CredentialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialResponse {
    private UUID id;
    private String issuerOrganization;
    private String holderName;
    private String holderEmail;
    private CredentialType credentialType;
    private String title;
    private String description;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String dataHash;
    private String previousChainHash;
    private String chainHash;
    private CredentialStatus status;
    private Instant createdAt;
    // Absolute URL the QR code encodes / the public link a recruiter can open directly.
    private String verificationUrl;
}
