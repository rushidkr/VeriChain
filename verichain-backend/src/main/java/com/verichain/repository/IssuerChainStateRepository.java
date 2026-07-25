package com.verichain.repository;

import com.verichain.entity.IssuerChainState;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface IssuerChainStateRepository extends JpaRepository<IssuerChainState, Long> {

    // Pessimistic lock so two concurrent issuance requests from the same issuer
    // can't both read the same "latest" link and create a fork in the chain.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IssuerChainState> findByIssuerId(Long issuerId);
}
