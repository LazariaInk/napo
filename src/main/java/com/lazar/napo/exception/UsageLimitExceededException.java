package com.lazar.napo.exception;

public class UsageLimitExceededException extends RuntimeException {

    public UsageLimitExceededException(String message) {
        super(message);
    }
}