package com.company.aa.exception;

/** Thrown for non-retryable errors returned by Digio (4xx business errors). Not retried by resilience4j. */
public class DigioClientException extends RuntimeException {
    public DigioClientException(String message) {
        super(message);
    }

    public DigioClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
