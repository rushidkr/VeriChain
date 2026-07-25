package com.verichain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "issuer_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "registration_number")
    private String registrationNumber;

    // Public key: safe to expose, used by anyone verifying a credential's signature.
    @Lob
    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    // Private key is AES-encrypted at rest (see KeyManagementService) and never returned via API.
    @Lob
    @Column(name = "private_key_encrypted", columnDefinition = "TEXT")
    private String privateKeyEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
