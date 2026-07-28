package com.elos.payments.payment_processing_service.controller;

import com.elos.payments.payment_processing_service.dto.PaymentRequestDTO;
import com.elos.payments.payment_processing_service.dto.ProviderRequestDTO;
import com.elos.payments.payment_processing_service.dto.ProcessingResponseDTO; // Import the response DTO
import com.elos.payments.payment_processing_service.entity.Transaction;
import com.elos.payments.payment_processing_service.repository.TransactionRepository;
import com.elos.payments.payment_processing_service.util.RsaSignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.security.PublicKey;
import java.util.UUID;

/**
 * Core controller for the Payment Processing Service. This acts as the second security gate,
 * verifying RSA signatures and managing the state of each transaction in the database.
 */
@RestController
@RequestMapping("/api/process")
public class ProcessingController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingController.class);

    private final RsaSignatureUtil rsaUtil;
    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;
    private final WebClient providerWebClient;

    @Autowired
    public ProcessingController(RsaSignatureUtil rsaUtil,
                                ObjectMapper objectMapper,
                                TransactionRepository transactionRepository,
                                @Value("${trustly.provider.url}") String providerServiceUrl) {
        this.rsaUtil = rsaUtil;
        this.objectMapper = objectMapper;
        this.transactionRepository = transactionRepository;
        this.providerWebClient = WebClient.create(providerServiceUrl);
        logger.info("Configured WebClient for Trustly Provider at: {}", providerServiceUrl);
    }

    /**
     * Processes a payment request after it has passed initial validation.
     * Flow:
     * 1. Verify the RSA signature against the raw request body (fail-fast security).
     * 2. If valid, create the official transaction record with 'CREATED' status.
     * 3. Forward the request to the specialized provider service.
     * 4. On success, update the transaction status to 'PENDING'.
     * 5. On failure, update the transaction status to 'FAILED' and propagate the error.
     */
    @PostMapping(value = "/payment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessingResponseDTO> processPayment(
            @RequestHeader("X-Signature") String signature,
            @RequestBody String payloadAsString) { // Receive the body as a raw String for verification.

        Transaction tx = null;

        try {
            // 1. Verify signature against the exact payload string received over the wire.
            PublicKey merchantPublicKey = rsaUtil.loadPublicKey("merchant_public_key.pem");
            if (!rsaUtil.verifySignature(payloadAsString, signature, merchantPublicKey)) {
                logger.error("Request REJECTED: Invalid RSA signature from upstream service.");
                ProcessingResponseDTO errorResponse = new ProcessingResponseDTO(null, null, "Invalid RSA signature", null);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 2. Only after security checks pass do we deserialize the payload.
            PaymentRequestDTO paymentRequest = objectMapper.readValue(payloadAsString, PaymentRequestDTO.class);
            logger.info("RSA Signature validated for payment from user: {}", paymentRequest.getUser().getEmail());

            // 3. Create the transaction record in the database.
            tx = new Transaction();
            tx.setMerchantTransactionId(paymentRequest.getMerchantTransactionId());
            tx.setTransactionReferenceId(UUID.randomUUID().toString());
            tx.setStatus("CREATED");
            transactionRepository.save(tx);
            logger.info("Transaction record created with ID [{}] and status: CREATED", tx.getTransactionReferenceId());

            // 4. Map to the provider-specific DTO and forward the request.
            ProviderRequestDTO providerRequest = new ProviderRequestDTO();
            providerRequest.setTransactionReferenceId(tx.getTransactionReferenceId());
            providerRequest.setAmount(paymentRequest.getAmount());
            providerRequest.setCurrency(paymentRequest.getCurrency());
            providerRequest.setCustomerEmail(paymentRequest.getUser().getEmail());

            logger.info("Forwarding request for transaction [{}] to Trustly Provider Service...", tx.getTransactionReferenceId());
            String providerResponseString = providerWebClient.post()
                    .uri("/api/provider/trustly/initiate")
                    .bodyValue(providerRequest)
                    .retrieve()
                    .onStatus(statusCode -> statusCode.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        logger.error("Downstream provider service returned an error. Status: {}, Body: {}", clientResponse.statusCode(), errorBody);
                                        return Mono.error(new WebClientResponseException(
                                                clientResponse.statusCode().value(), errorBody, null, errorBody.getBytes(), null
                                        ));
                                    })
                    )
                    .bodyToMono(String.class)
                    .block();

            Object providerResponseObject = objectMapper.readValue(providerResponseString, Object.class);

            // 5. Update status on success.
            tx.setStatus("PENDING");
            transactionRepository.save(tx);
            logger.info("Transaction [{}] status updated to PENDING after successful provider call.", tx.getTransactionReferenceId());

            ProcessingResponseDTO response = new ProcessingResponseDTO(
                    tx.getTransactionReferenceId(), tx.getStatus(), "Payment processing initiated.", providerResponseObject
            );
            return ResponseEntity.ok(response);

        } catch (WebClientResponseException ex) {
            logger.error("Call to Trustly Provider failed for transaction [{}].", (tx != null ? tx.getTransactionReferenceId() : "UNKNOWN"), ex);
            if (tx != null) {
                tx.setStatus("FAILED");
                transactionRepository.save(tx);
            }
            // Create a structured error response.
            ProcessingResponseDTO errorResponse = new ProcessingResponseDTO((tx != null ? tx.getTransactionReferenceId() : null), "FAILED", "Downstream service error.", ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);

        } catch (Exception e) {
            logger.error("An unexpected internal error occurred during payment processing for transaction [{}].", (tx != null ? tx.getTransactionReferenceId() : "UNKNOWN"), e);
            if (tx != null && tx.getId() != null) {
                tx.setStatus("FAILED");
                transactionRepository.save(tx);
            }
            ProcessingResponseDTO errorResponse = new ProcessingResponseDTO((tx != null ? tx.getTransactionReferenceId() : null), "FAILED", "An internal error occurred.", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}