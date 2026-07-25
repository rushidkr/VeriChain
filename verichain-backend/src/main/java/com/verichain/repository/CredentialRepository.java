package com.verichain.repository;

import com.verichain.entity.Credential;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    List<Credential> findByIssuerId(Long issuerId);

    Page<Credential> findByIssuerIdOrderByCreatedAtDesc(Long issuerId, Pageable pageable);

    Page<Credential> findByHolderEmailOrderByCreatedAtDesc(String holderEmail, Pageable pageable);
}
