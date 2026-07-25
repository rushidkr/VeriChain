package com.verichain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable: a lookup for a non-existent ID still gets logged (NOT_FOUND) for abuse monitoring.
    @Column(name = "credential_id")
    private UUID credentialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationResult result;

    @Column(name = "verifier_info")
    private String verifierInfo;

    @Column(name = "verified_at", nullable = false, updatable = false)
    private Instant verifiedAt;

    @PrePersist
    protected void onCreate() {
        this.verifiedAt = Instant.now();
    }
}
