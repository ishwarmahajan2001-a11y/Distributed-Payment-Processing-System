package com.elos.payments.payment_processing_service.dto;

import lombok.Data;

@Data
public class ProviderRequestDTO {
    private String transactionReferenceId;
    private double amount;
    private String currency;
    private String customerEmail;
}
