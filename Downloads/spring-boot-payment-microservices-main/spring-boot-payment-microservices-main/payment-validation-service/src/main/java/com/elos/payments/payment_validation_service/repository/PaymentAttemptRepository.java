package com.elos.payments.payment_validation_service.repository;


import com.elos.payments.payment_validation_service.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    // "SELECT COUNT(*) FROM payment_attempt WHERE email = ? AND created_date > ?"
    int countByEmailAndCreatedDateAfter(String email, LocalDateTime after);
}