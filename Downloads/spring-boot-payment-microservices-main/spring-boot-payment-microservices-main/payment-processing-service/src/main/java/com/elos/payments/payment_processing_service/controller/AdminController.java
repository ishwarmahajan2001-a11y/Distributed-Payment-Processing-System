package com.elos.payments.payment_processing_service.controller;

import com.elos.payments.payment_processing_service.dto.StatusNotificationDTO;
import com.elos.payments.payment_processing_service.entity.Transaction;
import com.elos.payments.payment_processing_service.repository.TransactionRepository;
import com.elos.payments.payment_processing_service.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin") // A separate endpoint for administrative tasks
public class AdminController {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NotificationService notificationService;

    // This endpoint will simulate receiving a final "APPROVED" or "FAILED" status.
    @PostMapping("/update-status")
    public ResponseEntity<String> updateStatus(
            @RequestParam String transactionReferenceId,
            @RequestParam String newStatus) {

        Optional<Transaction> txOptional = transactionRepository.findByTransactionReferenceId(transactionReferenceId);

        if (txOptional.isEmpty()) {
            return ResponseEntity.status(404).body("Transaction not found for ID: " + transactionReferenceId);
        }

        Transaction tx = txOptional.get();
        tx.setStatus(newStatus.toUpperCase());
        transactionRepository.save(tx);

        // After updating our database, send the notification to ActiveMQ
        StatusNotificationDTO notification = new StatusNotificationDTO(
                tx.getTransactionReferenceId(),
                tx.getStatus(),
                "Payment status has been updated to " + tx.getStatus()
        );
        notificationService.sendPaymentStatusUpdate(notification);

        return ResponseEntity.ok("Transaction " + transactionReferenceId + " status updated to " + newStatus + " and notification sent.");
    }
}