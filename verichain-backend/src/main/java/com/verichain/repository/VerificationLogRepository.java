package com.verichain.repository;

import com.verichain.entity.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {
    Page<VerificationLog> findAllByOrderByVerifiedAtDesc(Pageable pageable);
    List<VerificationLog> findByCredentialId(UUID credentialId);
}
