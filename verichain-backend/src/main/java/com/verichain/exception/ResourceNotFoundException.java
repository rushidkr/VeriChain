package com.verichain.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends VerichainException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
