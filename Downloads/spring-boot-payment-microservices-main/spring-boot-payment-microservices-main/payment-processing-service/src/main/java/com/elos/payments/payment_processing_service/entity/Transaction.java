package com.elos.payments.payment_processing_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import java.time.LocalDateTime;

@Entity // Tells Spring Boot this class is a blueprint for a database table.
@Data   // A Lombok helper that automatically creates getters, setters, toString(), etc.
public class Transaction {

    @Id // Marks this field as the primary key.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Tells the database to automatically generate this value (e.g., 1, 2, 3...).
    private Long id;

    private String merchantTransactionId; // The ID from the e-commerce client
    private String transactionReferenceId; // Our internal, unique ID (we will generate a UUID for this)
    private String status; // e.g., CREATED, PENDING, APPROVED

    private LocalDateTime createdDate = LocalDateTime.now();
}