package com.elos.payments.payment_validation_service.config;


import com.elos.payments.payment_validation_service.entity.ValidationRule;
import com.elos.payments.payment_validation_service.repository.ValidationRuleRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CacheInitializer implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(CacheInitializer.class);
    public static final String ACTIVE_RULES_KEY = "ACTIVE_VALIDATION_RULES";

    @Autowired
    private ValidationRuleRepository ruleRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @Override
    public void run(ApplicationArguments args) {
        refreshCache();
    }

    // Public method to allow refreshing the cache via an API call.
    public void refreshCache() {
        logger.info("Refreshing validation rules cache...");
        // First, delete any existing rules in the cache.
        redisTemplate.delete(ACTIVE_RULES_KEY);

        // Fetch all active rules from the database.
        List<ValidationRule> activeRules = ruleRepository.findByIsActiveTrue();

        if (!activeRules.isEmpty()) {
            // Get just the names of the rules.
            List<String> ruleNames = activeRules.stream().map(ValidationRule::getRuleName).toList();

            // Add all rule names to a list in Redis.
            redisTemplate.opsForList().rightPushAll(ACTIVE_RULES_KEY, ruleNames);
            logger.info("Loaded {} active validation rules into Redis cache.", ruleNames.size());
        } else {
            logger.warn("No active validation rules found in the database.");
        }
    }
}