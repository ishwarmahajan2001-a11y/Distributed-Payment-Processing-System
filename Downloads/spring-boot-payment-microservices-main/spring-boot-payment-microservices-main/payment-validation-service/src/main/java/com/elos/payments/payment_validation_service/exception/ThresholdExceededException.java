package com.elos.payments.payment_validation_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS) // HTTP Status 429
public class ThresholdExceededException extends RuntimeException {
    public ThresholdExceededException(String message) {
        super(message);
    }
}