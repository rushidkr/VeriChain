package com.verichain.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

/**
 * Pure hash-chain logic, deliberately kept free of JPA entities and Spring wiring so it can be
 * unit-tested in total isolation. This is the mechanism that makes tampering detectable:
 *
 *   dataHash   = SHA256(canonical credential fields)
 *   chainHash  = SHA256(dataHash + previousChainHash)
 *
 * Because each credential's chainHash depends on the previous one, altering ANY field of ANY
 * credential changes that credential's dataHash, which cascades and breaks every chainHash after
 * it in that issuer's sequence - a single-byte edit anywhere is detectable everywhere downstream.
 */
@Service
public class HashChainService {

    private static final String ALGORITHM = "SHA-256";

    /** Fields that go into the data hash - the "content" being fingerprinted. */
    public record CredentialData(
            String holderName,
            String holderEmail,
            String credentialType,
            String title,
            String description,
            LocalDate issueDate,
            LocalDate expiryDate,
            Long issuerId
    ) {}

    public String computeDataHash(CredentialData data) {
        String canonical = canonicalize(data);
        return sha256Hex(canonical);
    }

    /**
     * previousChainHash should be "" (empty string) for the very first credential issued
     * by a given issuer - i.e. the genesis link of that issuer's chain.
     */
    public String computeChainHash(String dataHash, String previousChainHash) {
        String linked = dataHash + "|" + (previousChainHash == null ? "" : previousChainHash);
        return sha256Hex(linked);
    }

    /**
     * Recomputes both hashes from scratch and compares against stored values - this is the
     * actual tamper check used at verification time. Returns true only if BOTH the content
     * hash and the chain link are intact.
     */
    public boolean isChainIntact(CredentialData data, String previousChainHash,
                                  String storedDataHash, String storedChainHash) {
        String recomputedDataHash = computeDataHash(data);
        if (!recomputedDataHash.equals(storedDataHash)) {
            return false;
        }
        String recomputedChainHash = computeChainHash(recomputedDataHash, previousChainHash);
        return recomputedChainHash.equals(storedChainHash);
    }

    private String canonicalize(CredentialData data) {
        // Fixed field order + explicit delimiters, so the same logical data always produces
        // the exact same string, regardless of how the object was constructed.
        return String.join("|",
                nullSafe(data.holderName()),
                nullSafe(data.holderEmail()),
                nullSafe(data.credentialType()),
                nullSafe(data.title()),
                nullSafe(data.description()),
                data.issueDate() == null ? "" : data.issueDate().toString(),
                data.expiryDate() == null ? "" : data.expiryDate().toString(),
                data.issuerId() == null ? "" : data.issuerId().toString()
        );
    }

    private String nullSafe(String s) {
        return s == null ? "" : s.trim();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
