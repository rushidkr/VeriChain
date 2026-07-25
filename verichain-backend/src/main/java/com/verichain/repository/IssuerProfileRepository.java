package com.verichain.repository;

import com.verichain.entity.ApprovalStatus;
import com.verichain.entity.IssuerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssuerProfileRepository extends JpaRepository<IssuerProfile, Long> {
    Optional<IssuerProfile> findByUserId(Long userId);
    List<IssuerProfile> findByApprovalStatus(ApprovalStatus status);
}
