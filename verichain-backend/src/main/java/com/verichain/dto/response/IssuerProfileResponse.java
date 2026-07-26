package com.verichain.dto.response;

import com.verichain.entity.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuerProfileResponse {
    private Long id;
    private String organizationName;
    private String registrationNumber;
    private String publicKey;
    private ApprovalStatus approvalStatus;
    private String approvedBy;
    private Instant approvedAt;
    private Instant createdAt;
}
