// src/main/java/com/elos/payments/payment_processing_service/dto/ProcessingResponseDTO.java

package com.elos.payments.payment_processing_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A DTO representing the structured JSON response from the Payment Processing Service.
 * This object is returned to the upstream service (Payment Validation Service).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingResponseDTO {
    private String transactionReferenceId;
    private String status;
    private String message;
    private Object providerResponse; // Using Object allows us to embed the flexible JSON from the provider service.
}