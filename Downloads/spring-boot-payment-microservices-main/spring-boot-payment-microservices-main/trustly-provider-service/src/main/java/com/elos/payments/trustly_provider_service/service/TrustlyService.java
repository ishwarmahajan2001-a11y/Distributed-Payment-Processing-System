package com.elos.payments.trustly_provider_service.service;

import com.elos.payments.trustly_provider_service.document.TrustlyApiLog;
import com.elos.payments.trustly_provider_service.dto.ProviderRequestDTO;
import com.elos.payments.trustly_provider_service.repository.TrustlyApiLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * This service acts as an adapter for the external Trustly API.
 * Its only job is to communicate with Trustly, logging every request and response for auditing.
 */
@Service
public class TrustlyService {

    private static final Logger logger = LoggerFactory.getLogger(TrustlyService.class);

    private final TrustlyApiLogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    // Using constructor injection for our dependencies.
    @Autowired
    public TrustlyService(TrustlyApiLogRepository logRepository,
                          ObjectMapper objectMapper,
                          @Value("${mock.trustly.url}") String mockTrustlyUrl) {
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;

        // The URL for the mock Trustly API is now loaded from config file.
        logger.info("Configuring WebClient to connect to Mock Trustly service at: {}", mockTrustlyUrl);
        this.webClient = WebClient.create(mockTrustlyUrl);
    }

    /**
     * Processes a payment request by forwarding it to the (mock) Trustly API.
     * Every request and its corresponding response are logged to MongoDB.
     * @param request The payment details from the processing service.
     * @return The raw response body from the Trustly API.
     * @throws Exception Allows exceptions (especially WebClientResponseException) to propagate up.
     */
    public String processTrustlyPayment(ProviderRequestDTO request) throws Exception {
        TrustlyApiLog logEntry = new TrustlyApiLog();
        logEntry.setTransactionReferenceId(request.getTransactionReferenceId());

        String requestPayload = objectMapper.writeValueAsString(request);
        logEntry.setRequestPayload(requestPayload);
        logger.info("Sending payment initiation request to mock Trustly for transaction ID: {}", request.getTransactionReferenceId());

        try {
            // Make the actual call to the mock service's /deposit endpoint.
            String responsePayload = webClient.post()
                    .uri("/deposit")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // If the call succeeds, log the response and save the complete record.
            logger.info("Received successful response from mock Trustly for transaction ID: {}", request.getTransactionReferenceId());
            logEntry.setResponsePayload(responsePayload);
            logRepository.save(logEntry);
            logger.info("Saved successful API log to MongoDB with ID: {}", logEntry.getId());

            return responsePayload;

        } catch (WebClientResponseException ex) {
            // This block runs if the mock service returns an error (e.g., 4xx, 5xx).
            logger.error("Call to mock Trustly service failed. Status: {}, Body: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);

            // Even on failure, we log the error response for auditing.
            logEntry.setResponsePayload("ERROR: " + ex.getResponseBodyAsString());
            logRepository.save(logEntry);
            logger.info("Saved FAILED API log to MongoDB with ID: {}", logEntry.getId());

            // It's important to re-throw the exception so the calling service (payment-processing)
            // knows that the call failed and can act accordingly (e.g., mark transaction as FAILED).
            throw ex;
        }
    }
}