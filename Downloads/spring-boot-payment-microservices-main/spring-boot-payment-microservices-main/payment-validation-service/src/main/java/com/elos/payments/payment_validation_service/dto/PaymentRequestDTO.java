package com.elos.payments.payment_validation_service.dto;

import lombok.Data;

@Data
public class PaymentRequestDTO {
    private String merchantTransactionId;
    private UserDTO user;
    private double amount;  
    private String currency;
}