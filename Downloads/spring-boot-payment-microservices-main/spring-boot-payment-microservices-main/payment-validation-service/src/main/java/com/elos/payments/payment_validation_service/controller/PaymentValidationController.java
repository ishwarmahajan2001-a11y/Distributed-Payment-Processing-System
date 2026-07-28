package com.elos.payments.payment_validation_service.controller;

import com.elos.payments.payment_validation_service.config.CacheInitializer;
import com.elos.payments.payment_validation_service.dto.PaymentRequestDTO;
import com.elos.payments.payment_validation_service.entity.PaymentAttempt;
import com.elos.payments.payment_validation_service.exception.ThresholdExceededException;
import com.elos.payments.payment_validation_service.repository.PaymentAttemptRepository;
import com.elos.payments.payment_validation_service.service.ValidationManager;
import com.elos.payments.payment_validation_service.util.RsaSignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.ContentCachingRequestWrapper;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

/**
 * The main entry point for the Payment Validation Service. This controller is the public front door
 * to our payment system, responsible for initial HMAC security, business rule validation,
 * and orchestrating the call to the next service in the chain.
 */
@RestController
@RequestMapping("/api/validate")
public class PaymentValidationController {

    private static final Logger log = LoggerFactory.getLogger(PaymentValidationController.class);

    private final PaymentAttemptRepository attemptRepository;
    private final ValidationManager validationManager;
    private final CacheInitializer cacheInitializer;
    private final RsaSignatureUtil rsaUtil;
    private final ObjectMapper objectMapper;
    private final WebClient processingServiceClient;

    @Autowired
    public PaymentValidationController(
            PaymentAttemptRepository attemptRepository,
            ValidationManager validationManager,
            CacheInitializer cacheInitializer,
            RsaSignatureUtil rsaUtil,
            ObjectMapper objectMapper,
            @Value("${payment.processing.url}") String processingServiceUrl) {
        this.attemptRepository = attemptRepository;
        this.validationManager = validationManager;
        this.cacheInitializer = cacheInitializer;
        this.rsaUtil = rsaUtil;
        this.objectMapper = objectMapper;
        this.processingServiceClient = WebClient.create(processingServiceUrl);
        log.info("Configured WebClient for Payment Processing Service at: {}", processingServiceUrl);
    }

    /**
     * Handles a new payment initiation request.
     * The flow is:
     * 1. Parse the request body (which was cached by the HmacFilter).
     * 2. Run all active business rules (loaded from Redis) BEFORE saving.
     * 3. If validation passes, log the payment attempt for auditing in its own transaction.
     * 4. Sign the request and forward it to the core processing service.
     */
    @PostMapping(value = "/payment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> validatePayment(HttpServletRequest request) {
        try {
            // 1. Read and Parse the Request Body
            ContentCachingRequestWrapper requestWrapper = (ContentCachingRequestWrapper) request;
            String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            PaymentRequestDTO paymentRequest = objectMapper.readValue(requestBody, PaymentRequestDTO.class);
            log.info("Received new payment validation request for user: {}", paymentRequest.getUser().getEmail());

            // 2. Execute Business Validations FIRST.
            validationManager.executeValidations(paymentRequest);
            log.info("Business validations passed for user: {}", paymentRequest.getUser().getEmail());

            // 3. Record the Attempt SECOND, in its own short-lived transaction.
            // This prevents holding a database lock during the long network call.
            recordAttempt(paymentRequest);

            // 4. Securely Forward to Processing Service
            PrivateKey merchantPrivateKey = rsaUtil.loadPrivateKey("merchant_private_key.pem");
            String signature = rsaUtil.generateSignature(requestBody, merchantPrivateKey);

            log.info("Forwarding signed request to the payment processing service...");
            Object processingResponse = processingServiceClient.post()
                    .uri("/api/process/payment")
                    .header("X-Signature", signature)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(statusCode -> statusCode.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody ->
                                    Mono.error(new WebClientResponseException(
                                            clientResponse.statusCode().value(), errorBody, null, null, null
                                    ))
                            )
                    )
                    .bodyToMono(Object.class)
                    .block();

            return ResponseEntity.ok(processingResponse);

        } catch (ThresholdExceededException ex) {
            log.warn("Payment rejected due to validation failure: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":\"" + ex.getMessage() + "\"}");
        } catch (WebClientResponseException ex) {
            log.error("Downstream service returned an error. Propagating. Status: {}, Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("An unexpected internal error occurred during payment validation.", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"An internal error occurred.\"}");
        }
    }

    /**
     * An admin endpoint to force a refresh of the validation rules from the DB into Redis.
     */
    @PostMapping("/rules/refresh")
    public ResponseEntity<String> refreshRulesCache() {
        log.info("Manual cache refresh triggered via API.");
        cacheInitializer.refreshCache();
        return ResponseEntity.ok("Validation rules cache has been refreshed successfully.");
    }

    /**
     * A private helper method to save the payment attempt in its own, short-lived transaction.
     * Propagation.REQUIRES_NEW ensures this runs in a new, independent transaction that commits immediately,
     * releasing the database lock before we make the long network call.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void recordAttempt(PaymentRequestDTO paymentRequest) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.setEmail(paymentRequest.getUser().getEmail());
        attemptRepository.save(attempt);
        log.info("Successfully recorded payment attempt for user: {}", paymentRequest.getUser().getEmail());
    }
}