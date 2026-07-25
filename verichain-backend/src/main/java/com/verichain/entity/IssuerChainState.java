package com.verichain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "issuer_chain_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuerChainState {

    @Id
    @Column(name = "issuer_id")
    private Long issuerId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "issuer_id")
    private IssuerProfile issuer;

    @Column(name = "latest_chain_hash", length = 128)
    private String latestChainHash;

    @Column(name = "sequence_number", nullable = false)
    @Builder.Default
    private Long sequenceNumber = 0L;
}
