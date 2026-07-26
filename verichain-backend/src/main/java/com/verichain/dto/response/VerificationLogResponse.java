package com.verichain.dto.response;

import com.verichain.entity.VerificationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationLogResponse {
    private Long id;
    private UUID credentialId;
    private VerificationResult result;
    private String verifierInfo;
    private Instant verifiedAt;
}
