package com.verichain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SignatureServiceTest {

    private final SignatureService signatureService = new SignatureService();

    private String privateKeyBase64;
    private String publicKeyBase64;

    @BeforeEach
    void generateTestKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    @Test
    void validSignature_verifiesSuccessfully() {
        String data = "abc123chainhash";
        String signature = signatureService.sign(data, privateKeyBase64);

        assertTrue(signatureService.verify(data, signature, publicKeyBase64));
    }

    @Test
    void tamperedData_failsVerification() {
        String originalData = "abc123chainhash";
        String signature = signatureService.sign(originalData, privateKeyBase64);

        String tamperedData = "abc123chainhashXYZ";
        assertFalse(signatureService.verify(tamperedData, signature, publicKeyBase64));
    }

    @Test
    void wrongPublicKey_failsVerification() throws Exception {
        String data = "abc123chainhash";
        String signature = signatureService.sign(data, privateKeyBase64);

        // A different issuer's public key should not validate this signature.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String otherPublicKey = Base64.getEncoder().encodeToString(generator.generateKeyPair().getPublic().getEncoded());

        assertFalse(signatureService.verify(data, signature, otherPublicKey));
    }

    @Test
    void forgedSignature_failsVerification() {
        String data = "abc123chainhash";
        String forgedSignature = Base64.getEncoder().encodeToString("not-a-real-signature".getBytes());

        assertFalse(signatureService.verify(data, forgedSignature, publicKeyBase64));
    }

    @Test
    void malformedSignatureString_doesNotThrow_returnsFalse() {
        String data = "abc123chainhash";
        assertFalse(signatureService.verify(data, "!!!not-base64!!!", publicKeyBase64));
    }
}
