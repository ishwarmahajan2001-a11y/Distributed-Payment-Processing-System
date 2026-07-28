package com.elos.payments.payment_validation_service.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data; // Needed for @Data
import java.time.LocalDateTime;


@Entity // CRITICAL: Tells Spring Boot this class maps to a database table.
@Data   // CRITICAL: A Lombok helper that automatically creates getters, setters, etc.
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tells the database to auto-generate this value.
    private Long id;
    private String email;
    private LocalDateTime createdDate = LocalDateTime.now();
}