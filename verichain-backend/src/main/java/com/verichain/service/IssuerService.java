package com.verichain.service;

import com.verichain.dto.response.IssuerProfileResponse;
import com.verichain.entity.IssuerProfile;
import com.verichain.exception.ResourceNotFoundException;
import com.verichain.repository.IssuerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssuerService {

    private final IssuerProfileRepository issuerProfileRepository;

    @Transactional(readOnly = true)
    public IssuerProfileResponse getOwnProfile(Long userId) {
        IssuerProfile profile = issuerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Issuer profile not found"));
        return toResponse(profile);
    }

    private IssuerProfileResponse toResponse(IssuerProfile p) {
        return IssuerProfileResponse.builder()
                .id(p.getId())
                .organizationName(p.getOrganizationName())
                .registrationNumber(p.getRegistrationNumber())
                .publicKey(p.getPublicKey())
                .approvalStatus(p.getApprovalStatus())
                .approvedBy(p.getApprovedBy())
                .approvedAt(p.getApprovedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
