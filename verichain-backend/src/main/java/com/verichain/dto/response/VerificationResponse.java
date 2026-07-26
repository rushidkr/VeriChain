package com.verichain.dto.response;

import com.verichain.entity.CredentialType;
import com.verichain.entity.VerificationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {

    private VerificationResult result;
    private String message;

    // Populated only when the credential exists (regardless of whether it's valid/tampered),
    // so a verifier can see WHAT is being checked, not just the verdict.
    private UUID credentialId;
    private String issuerOrganization;
    private String holderName;
    private CredentialType credentialType;
    private String title;
    private LocalDate issueDate;
    private LocalDate expiryDate;

    private boolean chainIntact;
    private boolean signatureValid;
}
