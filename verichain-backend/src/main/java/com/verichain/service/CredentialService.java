package com.verichain.service;

import com.verichain.dto.request.CredentialIssueRequest;
import com.verichain.dto.response.CredentialResponse;
import com.verichain.entity.*;
import com.verichain.exception.ResourceNotFoundException;
import com.verichain.exception.VerichainException;
import com.verichain.repository.CredentialRepository;
import com.verichain.repository.IssuerChainStateRepository;
import com.verichain.repository.IssuerProfileRepository;
import com.verichain.repository.RevocationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final IssuerProfileRepository issuerProfileRepository;
    private final IssuerChainStateRepository issuerChainStateRepository;
    private final RevocationRecordRepository revocationRecordRepository;
    private final HashChainService hashChainService;
    private final SignatureService signatureService;
    private final KeyManagementService keyManagementService;
    private final QrCodeService qrCodeService;
    private final ChainStateInitializer chainStateInitializer;

    /**
     * Issues a new credential: computes its content hash, links it to the
     * issuer's previous chain link, signs the resulting chain hash with the
     * issuer's private key, and persists everything atomically. The
     * IssuerChainState row is pessimistically locked for the duration of the
     * transaction so concurrent issuance requests from the same issuer can't
     * both read the same "latest" link and fork the chain. First-time row
     * creation is handled separately (see ChainStateInitializer) so that race
     * doesn't poison this transaction.
     */
    @Transactional
    public CredentialResponse issueCredential(Long issuerUserId, CredentialIssueRequest request) {
        IssuerProfile issuer = getApprovedIssuerOrThrow(issuerUserId);

        chainStateInitializer.ensureExists(issuer);

        IssuerChainState chainState = issuerChainStateRepository.findByIssuerId(issuer.getId())
                .orElseThrow(() -> new IllegalStateException(
                "IssuerChainState missing for issuer " + issuer.getId() + " after initialization"));

        String previousChainHash = chainState.getLatestChainHash() == null ? "" : chainState.getLatestChainHash();

        HashChainService.CredentialData data = new HashChainService.CredentialData(
                request.getHolderName(),
                request.getHolderEmail(),
                request.getCredentialType().name(),
                request.getTitle(),
                request.getDescription(),
                request.getIssueDate(),
                request.getExpiryDate(),
                issuer.getId()
        );

        String dataHash = hashChainService.computeDataHash(data);
        String chainHash = hashChainService.computeChainHash(dataHash, previousChainHash);

        String privateKey = keyManagementService.decryptPrivateKey(issuer.getPrivateKeyEncrypted());
        String signature = signatureService.sign(chainHash, privateKey);

        Credential credential = Credential.builder()
                .issuer(issuer)
                .holderName(request.getHolderName())
                .holderEmail(request.getHolderEmail())
                .credentialType(request.getCredentialType())
                .title(request.getTitle())
                .description(request.getDescription())
                .issueDate(request.getIssueDate())
                .expiryDate(request.getExpiryDate())
                .dataHash(dataHash)
                .previousChainHash(previousChainHash)
                .chainHash(chainHash)
                .signature(signature)
                .status(CredentialStatus.ACTIVE)
                .build();

        credential = credentialRepository.save(credential);

        chainState.setLatestChainHash(chainHash);
        chainState.setSequenceNumber(chainState.getSequenceNumber() + 1);
        issuerChainStateRepository.save(chainState);

        return toResponse(credential);
    }

    @Transactional(readOnly = true)
    public Page<CredentialResponse> listCredentialsForIssuer(Long issuerUserId, Pageable pageable) {
        IssuerProfile issuer = getIssuerProfileOrThrow(issuerUserId);
        return credentialRepository.findByIssuerIdOrderByCreatedAtDesc(issuer.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CredentialResponse> listCredentialsForStudent(String holderEmail, Pageable pageable) {
        return credentialRepository.findByHolderEmailOrderByCreatedAtDesc(holderEmail, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CredentialResponse getCredentialForIssuer(Long issuerUserId, UUID credentialId) {
        IssuerProfile issuer = getIssuerProfileOrThrow(issuerUserId);
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found: " + credentialId));

        if (!credential.getIssuer().getId().equals(issuer.getId())) {
            throw new VerichainException("This credential does not belong to your organization", HttpStatus.FORBIDDEN);
        }
        return toResponse(credential);
    }

    @Transactional
    public void revokeCredential(Long issuerUserId, UUID credentialId, String reason) {
        IssuerProfile issuer = getIssuerProfileOrThrow(issuerUserId);
        Credential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found: " + credentialId));

        if (!credential.getIssuer().getId().equals(issuer.getId())) {
            throw new VerichainException("This credential does not belong to your organization", HttpStatus.FORBIDDEN);
        }
        if (credential.getStatus() == CredentialStatus.REVOKED) {
            throw new VerichainException("Credential is already revoked", HttpStatus.CONFLICT);
        }

        credential.setStatus(CredentialStatus.REVOKED);
        credentialRepository.save(credential);

        RevocationRecord record = RevocationRecord.builder()
                .credentialId(credentialId)
                .revokedBy(issuer.getOrganizationName())
                .reason(reason == null || reason.isBlank() ? "Not specified" : reason)
                .build();
        revocationRecordRepository.save(record);
    }

    private IssuerProfile getApprovedIssuerOrThrow(Long userId) {
        IssuerProfile issuer = getIssuerProfileOrThrow(userId);
        if (issuer.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new VerichainException(
                    "Your issuer account is not yet approved by admin - cannot issue credentials",
                    HttpStatus.FORBIDDEN);
        }
        return issuer;
    }

    private IssuerProfile getIssuerProfileOrThrow(Long userId) {
        return issuerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Issuer profile not found for this account"));
    }

    private CredentialResponse toResponse(Credential c) {
        return CredentialResponse.builder()
                .id(c.getId())
                .issuerOrganization(c.getIssuer().getOrganizationName())
                .holderName(c.getHolderName())
                .holderEmail(c.getHolderEmail())
                .credentialType(c.getCredentialType())
                .title(c.getTitle())
                .description(c.getDescription())
                .issueDate(c.getIssueDate())
                .expiryDate(c.getExpiryDate())
                .dataHash(c.getDataHash())
                .previousChainHash(c.getPreviousChainHash())
                .chainHash(c.getChainHash())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .verificationUrl(qrCodeService.buildVerificationUrl(c.getId()))
                .build();
    }
}
