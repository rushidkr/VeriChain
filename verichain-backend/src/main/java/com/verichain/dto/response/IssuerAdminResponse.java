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
public class IssuerAdminResponse {
    private Long issuerId;
    private Long userId;
    private String organizationName;
    private String registrationNumber;
    private String contactName;
    private String contactEmail;
    private ApprovalStatus approvalStatus;
    private Instant createdAt;
}
