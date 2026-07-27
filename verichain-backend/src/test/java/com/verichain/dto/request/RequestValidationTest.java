package com.verichain.dto.request;

import com.verichain.entity.CredentialType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void registerRequestShouldRejectPasswordWithoutUppercase() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("abc12345");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .anyMatch(message -> message.contains("Password must include uppercase"));
    }

    @Test
    void credentialIssueRequestShouldRejectExpiryBeforeIssueDate() {
        CredentialIssueRequest request = new CredentialIssueRequest();
        request.setHolderName("Jane Doe");
        request.setHolderEmail("jane@example.com");
        request.setCredentialType(CredentialType.DEGREE);
        request.setTitle("Computer Science Degree");
        request.setIssueDate(LocalDate.of(2025, 6, 1));
        request.setExpiryDate(LocalDate.of(2025, 5, 1));

        Set<ConstraintViolation<CredentialIssueRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .anyMatch(message -> message.contains("Expiry date"));
    }
}
