package com.verichain.controller;

import com.verichain.dto.request.CredentialIssueRequest;
import com.verichain.dto.request.RevokeRequest;
import com.verichain.dto.response.CredentialResponse;
import com.verichain.dto.response.IssuerProfileResponse;
import com.verichain.security.UserPrincipal;
import com.verichain.service.CredentialService;
import com.verichain.service.IssuerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/issuer")
@RequiredArgsConstructor
public class IssuerController {

    private final IssuerService issuerService;
    private final CredentialService credentialService;

    @GetMapping("/profile")
    public ResponseEntity<IssuerProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(issuerService.getOwnProfile(principal.getId()));
    }

    @PostMapping("/credentials")
    public ResponseEntity<CredentialResponse> issueCredential(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CredentialIssueRequest request) {
        CredentialResponse response = credentialService.issueCredential(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/credentials")
    public ResponseEntity<Page<CredentialResponse>> listCredentials(@AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(credentialService.listCredentialsForIssuer(principal.getId(), pageable));
    }

    @GetMapping("/credentials/{id}")
    public ResponseEntity<CredentialResponse> getCredential(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(credentialService.getCredentialForIssuer(principal.getId(), id));
    }

    @PutMapping("/credentials/{id}/revoke")
    public ResponseEntity<Void> revokeCredential(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) RevokeRequest request) {
        String reason = request != null ? request.getReason() : null;
        credentialService.revokeCredential(principal.getId(), id, reason);
        return ResponseEntity.noContent().build();
    }
}
