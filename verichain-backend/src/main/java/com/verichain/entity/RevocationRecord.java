package com.verichain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "revocation_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevocationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credential_id", nullable = false, unique = true)
    private UUID credentialId;

    @Column(name = "revoked_by", nullable = false)
    private String revokedBy;

    @Column(nullable = false)
    private String reason;

    @Column(name = "revoked_at", nullable = false, updatable = false)
    private Instant revokedAt;

    @PrePersist
    protected void onCreate() {
        this.revokedAt = Instant.now();
    }
}
