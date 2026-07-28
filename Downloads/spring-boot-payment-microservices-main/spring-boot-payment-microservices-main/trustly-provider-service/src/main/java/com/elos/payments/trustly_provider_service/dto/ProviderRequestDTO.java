package com.elos.payments.trustly_provider_service.dto;


import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;



@Data // Creates getters, setters, toString(), etc.
@NoArgsConstructor
@AllArgsConstructor
public class ProviderRequestDTO {
    private String transactionReferenceId;
    private double amount;
    private String currency;
    private String customerEmail;
}