package com.verichain.controller;

import com.verichain.dto.response.IssuerAdminResponse;
import com.verichain.dto.response.VerificationLogResponse;
import com.verichain.security.UserPrincipal;
import com.verichain.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/issuers/pending")
    public ResponseEntity<List<IssuerAdminResponse>> pendingIssuers() {
        return ResponseEntity.ok(adminService.listPendingIssuers());
    }

    @GetMapping("/issuers")
    public ResponseEntity<List<IssuerAdminResponse>> allIssuers() {
        return ResponseEntity.ok(adminService.listAllIssuers());
    }

    @PutMapping("/issuers/{id}/approve")
    public ResponseEntity<Void> approveIssuer(@PathVariable Long id,
                                              @AuthenticationPrincipal UserPrincipal admin) {
        adminService.approveIssuer(id, admin.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/issuers/{id}/reject")
    public ResponseEntity<Void> rejectIssuer(@PathVariable Long id,
                                              @AuthenticationPrincipal UserPrincipal admin) {
        adminService.rejectIssuer(id, admin.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verification-logs")
    public ResponseEntity<Page<VerificationLogResponse>> verificationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.getVerificationLogs(pageable));
    }
}
