package com.verichain.service;

import com.verichain.dto.response.IssuerAdminResponse;
import com.verichain.dto.response.VerificationLogResponse;
import com.verichain.entity.ApprovalStatus;
import com.verichain.entity.IssuerProfile;
import com.verichain.exception.ResourceNotFoundException;
import com.verichain.exception.VerichainException;
import com.verichain.repository.IssuerProfileRepository;
import com.verichain.repository.VerificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final IssuerProfileRepository issuerProfileRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final KeyManagementService keyManagementService;

    @Transactional(readOnly = true)
    public List<IssuerAdminResponse> listPendingIssuers() {
        return issuerProfileRepository.findByApprovalStatus(ApprovalStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssuerAdminResponse> listAllIssuers() {
        return issuerProfileRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Approving an issuer is the moment their RSA keypair is generated - deliberately deferred
     * until now (rather than at registration) so a rejected/unvetted applicant never holds a
     * signing key capable of issuing "valid-looking" credentials.
     */
    @Transactional
    public void approveIssuer(Long issuerId, String adminName) {
        IssuerProfile issuer = issuerProfileRepository.findById(issuerId)
                .orElseThrow(() -> new ResourceNotFoundException("Issuer not found: " + issuerId));

        if (issuer.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new VerichainException("Issuer is already approved", HttpStatus.CONFLICT);
        }

        KeyManagementService.GeneratedKeyPair keyPair = keyManagementService.generateIssuerKeyPair();

        issuer.setPublicKey(keyPair.publicKeyBase64());
        issuer.setPrivateKeyEncrypted(keyPair.privateKeyEncrypted());
        issuer.setApprovalStatus(ApprovalStatus.APPROVED);
        issuer.setApprovedBy(adminName);
        issuer.setApprovedAt(Instant.now());

        issuerProfileRepository.save(issuer);
    }

    @Transactional
    public void rejectIssuer(Long issuerId, String adminName) {
        IssuerProfile issuer = issuerProfileRepository.findById(issuerId)
                .orElseThrow(() -> new ResourceNotFoundException("Issuer not found: " + issuerId));

        issuer.setApprovalStatus(ApprovalStatus.REJECTED);
        issuer.setApprovedBy(adminName);
        issuer.setApprovedAt(Instant.now());

        issuerProfileRepository.save(issuer);
    }

    @Transactional(readOnly = true)
    public Page<VerificationLogResponse> getVerificationLogs(Pageable pageable) {
        return verificationLogRepository.findAllByOrderByVerifiedAtDesc(pageable)
                .map(log -> VerificationLogResponse.builder()
                        .id(log.getId())
                        .credentialId(log.getCredentialId())
                        .result(log.getResult())
                        .verifierInfo(log.getVerifierInfo())
                        .verifiedAt(log.getVerifiedAt())
                        .build());
    }

    private IssuerAdminResponse toResponse(IssuerProfile p) {
        return IssuerAdminResponse.builder()
                .issuerId(p.getId())
                .userId(p.getUser().getId())
                .organizationName(p.getOrganizationName())
                .registrationNumber(p.getRegistrationNumber())
                .contactName(p.getUser().getName())
                .contactEmail(p.getUser().getEmail())
                .approvalStatus(p.getApprovalStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
