package com.verichain.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class HashChainServiceTest {

    private final HashChainService hashChainService = new HashChainService();

    private HashChainService.CredentialData sampleData() {
        return new HashChainService.CredentialData(
                "Rushi Patil",
                "rushi@example.com",
                "INTERNSHIP_CERTIFICATE",
                "Software Engineering Intern",
                "Completed 6-month internship in backend development",
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2027, 1, 15),
                1L
        );
    }

    @Test
    void sameInput_producesSameHash_deterministic() {
        String hash1 = hashChainService.computeDataHash(sampleData());
        String hash2 = hashChainService.computeDataHash(sampleData());
        assertEquals(hash1, hash2, "Hashing the same data twice must produce identical hashes");
    }

    @Test
    void differentInput_producesDifferentHash() {
        var original = sampleData();
        var modified = new HashChainService.CredentialData(
                original.holderName(),
                original.holderEmail(),
                original.credentialType(),
                "Senior Software Engineering Intern", // title changed
                original.description(),
                original.issueDate(),
                original.expiryDate(),
                original.issuerId()
        );

        String hash1 = hashChainService.computeDataHash(original);
        String hash2 = hashChainService.computeDataHash(modified);
        assertNotEquals(hash1, hash2, "Even a single-field change must change the hash");
    }

    @Test
    void chainHash_dependsOnPreviousHash() {
        String dataHash = hashChainService.computeDataHash(sampleData());
        String chain1 = hashChainService.computeChainHash(dataHash, "");
        String chain2 = hashChainService.computeChainHash(dataHash, "someOtherPreviousHash");
        assertNotEquals(chain1, chain2, "Chain hash must depend on the previous link");
    }

    @Test
    void isChainIntact_returnsTrue_whenNothingTampered() {
        var data = sampleData();
        String dataHash = hashChainService.computeDataHash(data);
        String previousChainHash = "genesis";
        String chainHash = hashChainService.computeChainHash(dataHash, previousChainHash);

        assertTrue(hashChainService.isChainIntact(data, previousChainHash, dataHash, chainHash));
    }

    @Test
    void isChainIntact_returnsFalse_whenDataTampered() {
        var data = sampleData();
        String dataHash = hashChainService.computeDataHash(data);
        String previousChainHash = "genesis";
        String chainHash = hashChainService.computeChainHash(dataHash, previousChainHash);

        // Simulate someone editing the title directly in the database after issuance.
        var tamperedData = new HashChainService.CredentialData(
                data.holderName(), data.holderEmail(), data.credentialType(),
                "FAKE Senior Intern Title", data.description(),
                data.issueDate(), data.expiryDate(), data.issuerId()
        );

        assertFalse(hashChainService.isChainIntact(tamperedData, previousChainHash, dataHash, chainHash),
                "Tampering with any field must be detected");
    }

    @Test
    void isChainIntact_returnsFalse_whenChainHashTampered() {
        var data = sampleData();
        String dataHash = hashChainService.computeDataHash(data);
        String previousChainHash = "genesis";

        // Data hash matches, but someone forged the stored chainHash directly.
        String forgedChainHash = "0000000000000000000000000000000000000000000000000000000000000000";

        assertFalse(hashChainService.isChainIntact(data, previousChainHash, dataHash, forgedChainHash));
    }

    @Test
    void isChainIntact_returnsFalse_whenPreviousLinkTampered() {
        var data = sampleData();
        String dataHash = hashChainService.computeDataHash(data);
        String realPreviousHash = "realPreviousChainHash";
        String chainHash = hashChainService.computeChainHash(dataHash, realPreviousHash);

        // Someone tries to re-splice this credential onto a different point in the chain.
        String forgedPreviousHash = "differentPreviousChainHash";

        assertFalse(hashChainService.isChainIntact(data, forgedPreviousHash, dataHash, chainHash),
                "Re-linking a credential to a different previous hash must be detected");
    }

    @Test
    void dataHash_isValidSha256Hex() {
        String hash = hashChainService.computeDataHash(sampleData());
        assertEquals(64, hash.length(), "SHA-256 hex digest must be 64 characters");
        assertTrue(hash.matches("[0-9a-f]+"), "Hash must be lowercase hex");
    }
}
