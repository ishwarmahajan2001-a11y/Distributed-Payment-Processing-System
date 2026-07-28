package com.elos.payments.payment_validation_service.validators;


import com.elos.payments.payment_validation_service.dto.PaymentRequestDTO;

public interface Validator {
    void validate(PaymentRequestDTO paymentRequest);
    String getRuleName();
}
