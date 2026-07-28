package com.elos.payments.payment_processing_service.repository;

import com.elos.payments.payment_processing_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface TransactionRepository extends JpaRepository<Transaction, Long> {


    Optional<Transaction> findByTransactionReferenceId(String transactionReferenceId);
}