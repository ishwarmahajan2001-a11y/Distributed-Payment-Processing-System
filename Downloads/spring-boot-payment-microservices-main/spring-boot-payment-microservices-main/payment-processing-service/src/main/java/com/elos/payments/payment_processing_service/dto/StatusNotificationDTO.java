package com.elos.payments.payment_processing_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusNotificationDTO {
    private String transactionReferenceId;
    private String status;
    private String message;
}