package com.elos.payments.payment_processing_service.dto;

import lombok.Data;

@Data
public class PaymentRequestDTO {
    private String merchantTransactionId;
    private UserDTO user;
    private double amount;
    private String currency;
}