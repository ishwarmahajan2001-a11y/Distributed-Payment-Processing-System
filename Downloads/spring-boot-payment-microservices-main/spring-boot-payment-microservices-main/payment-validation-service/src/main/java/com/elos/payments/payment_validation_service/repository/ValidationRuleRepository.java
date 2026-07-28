package com.elos.payments.payment_validation_service.repository;


import com.elos.payments.payment_validation_service.entity.ValidationRule;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ValidationRuleRepository extends JpaRepository<ValidationRule, String> {
    // Find all rules where the 'isActive' column is true
    List<ValidationRule> findByIsActiveTrue();
}