package com.verichain.exception;

import org.springframework.http.HttpStatus;

public class VerichainException extends RuntimeException {

    private final HttpStatus status;

    public VerichainException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
