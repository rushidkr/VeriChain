package com.verichain.repository;

import com.verichain.entity.RevocationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RevocationRecordRepository extends JpaRepository<RevocationRecord, Long> {
    Optional<RevocationRecord> findByCredentialId(UUID credentialId);
}
