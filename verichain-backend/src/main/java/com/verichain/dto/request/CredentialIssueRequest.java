package com.verichain.dto.request;

import com.verichain.entity.CredentialType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CredentialIssueRequest {

    @NotBlank(message = "Holder name is required")
    @Size(max = 120, message = "Holder name must be at most 120 characters")
    private String holderName;

    @NotBlank(message = "Holder email is required")
    @Email(message = "Holder email must be valid")
    private String holderEmail;

    @NotNull(message = "Credential type is required")
    private CredentialType credentialType;

    @NotBlank(message = "Title is required")
    @Size(max = 160, message = "Title must be at most 160 characters")
    private String title;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotNull(message = "Issue date is required")
    @PastOrPresent(message = "Issue date cannot be in the future")
    private LocalDate issueDate;

    // Optional - null means "does not expire" (e.g. a degree)
    private LocalDate expiryDate;

    @AssertTrue(message = "Expiry date cannot be earlier than the issue date")
    public boolean isExpiryDateValid() {
        if (issueDate == null || expiryDate == null) {
            return true;
        }
        return !expiryDate.isBefore(issueDate);
    }
}
