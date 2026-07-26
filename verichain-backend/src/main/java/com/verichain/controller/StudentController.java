package com.verichain.controller;

import com.verichain.dto.response.CredentialResponse;
import com.verichain.security.UserPrincipal;
import com.verichain.service.CredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final CredentialService credentialService;

    @GetMapping("/credentials")
    public ResponseEntity<Page<CredentialResponse>> listCredentials(@AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(credentialService.listCredentialsForStudent(principal.getUsername(), pageable));
    }
}
