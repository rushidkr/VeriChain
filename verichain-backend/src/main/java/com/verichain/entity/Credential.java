package com.verichain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credential {

    @Id
    @Builder.Default
    @Column(updatable = false, nullable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id", nullable = false)
    private IssuerProfile issuer;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Column(name = "holder_email", nullable = false)
    private String holderEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false)
    private CredentialType credentialType;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // SHA-256 hash of the canonical credential fields above.
    @Column(name = "data_hash", nullable = false, length = 128)
    private String dataHash;

    // Chain hash of the issuer's PREVIOUS credential (null/"" for the first credential in the chain).
    @Column(name = "previous_chain_hash", length = 128)
    private String previousChainHash;

    // SHA256(dataHash + previousChainHash) - this record's link in the chain.
    @Column(name = "chain_hash", nullable = false, length = 128)
    private String chainHash;

    // Issuer's digital signature over chainHash, using their private key.
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CredentialStatus status = CredentialStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
