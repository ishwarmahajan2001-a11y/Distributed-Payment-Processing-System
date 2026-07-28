package com.elos.payments.payment_validation_service.service;


import com.elos.payments.payment_validation_service.config.CacheInitializer;
import com.elos.payments.payment_validation_service.dto.PaymentRequestDTO;
import com.elos.payments.payment_validation_service.validators.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ValidationManager {
    private static final Logger logger = LoggerFactory.getLogger(ValidationManager.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ApplicationContext context; // The Spring context, used to get beans by name.

    public void executeValidations(PaymentRequestDTO paymentRequest) {
        // Get the list of active rule names from the Redis cache.
        List<String> activeRuleNames = redisTemplate.opsForList().range(CacheInitializer.ACTIVE_RULES_KEY, 0, -1);

        if (activeRuleNames == null || activeRuleNames.isEmpty()) {
            logger.warn("No validation rules are active in the cache. Skipping validation.");
            return;
        }

        logger.info("Executing active validation rules: {}", activeRuleNames);


        for (String ruleName : activeRuleNames) {
            try {

                Validator validator = context.getBean(ruleName, Validator.class);
                validator.validate(paymentRequest);
            } catch (Exception e) {

                logger.error("Validation failed for rule: [{}]. Reason: {}", ruleName, e.getMessage());
                throw e;
            }
        }
    }
}