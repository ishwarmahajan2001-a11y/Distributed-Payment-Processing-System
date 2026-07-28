package com.elos.payments.payment_validation_service.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ValidationRule {
    @Id
    private String ruleName; // e.g., "paymentAttemptThresholdValidator"
    private boolean isActive;
}
