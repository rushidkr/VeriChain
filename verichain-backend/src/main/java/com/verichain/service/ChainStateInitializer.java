package com.verichain.service;

import com.verichain.entity.IssuerChainState;
import com.verichain.entity.IssuerProfile;
import com.verichain.repository.IssuerChainStateRepository;
import com.verichain.repository.IssuerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deliberately its own Spring bean, not a private method on CredentialService.
 *
 * Why: Spring's @Transactional works by wrapping a bean in a proxy. Calling another
 * @Transactional method on `this` from within the same class bypasses that proxy entirely,
 * so a self-invoked @Transactional(REQUIRES_NEW) would silently run in the CALLER's
 * transaction instead of its own - not a compile error, just quietly wrong at runtime.
 * Putting this in a separate bean forces the call to go through the real proxy.
 *
 * What it solves: two concurrent "first ever credential" requests for the same issuer could
 * both see no IssuerChainState row and both try to INSERT one. Running that insert attempt in
 * its own REQUIRES_NEW transaction means a duplicate-key failure here only rolls back this
 * tiny transaction, not the caller's - the caller just re-reads and finds the winner's row.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChainStateInitializer {

    private final IssuerChainStateRepository issuerChainStateRepository;
    private final IssuerProfileRepository issuerProfileRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(IssuerProfile issuer) {
        if (issuerChainStateRepository.existsById(issuer.getId())) {
            return;
        }
        try {
            IssuerProfile managedIssuer = issuerProfileRepository.findById(issuer.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Issuer not found: " + issuer.getId()));

            issuerChainStateRepository.save(
                    IssuerChainState.builder()
                            .issuer(managedIssuer)
                            .latestChainHash("")
                            .sequenceNumber(0L)
                            .build());
        } catch (DataIntegrityViolationException e) {
            // Another concurrent request created it first - that's fine, this is exactly
            // the race this method exists to absorb. The caller will simply read the row
            // that won.
            log.debug("IssuerChainState for issuer {} was created concurrently by another request", issuer.getId());
        }
    }
}
