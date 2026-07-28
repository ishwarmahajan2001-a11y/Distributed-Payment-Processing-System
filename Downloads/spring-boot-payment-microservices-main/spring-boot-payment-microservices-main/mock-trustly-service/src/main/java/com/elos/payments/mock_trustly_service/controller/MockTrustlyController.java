package com.elos.payments.mock_trustly_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;


@RestController
public class MockTrustlyController {

    private static final Logger logger = LoggerFactory.getLogger(MockTrustlyController.class);

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(@RequestBody JsonNode payload) {
        // Using JsonNode is a good practice for a mock, as it accepts any valid JSON.
        String txId = payload.has("transactionReferenceId") ? payload.get("transactionReferenceId").asText() : "UNKNOWN";
        logger.info("--> MOCK TRUSTLY SERVICE: Received deposit request for transaction ID: {}", txId);

        // Create a fake successful response that includes a redirect URL.
        String redirectUrl = "https://checkout.mock-trustly.com/session/" + UUID.randomUUID();

        Map<String, Object> responseData = Map.of(
                "redirectUrl", redirectUrl,
                "providerReferenceId", "trustly-tx-" + UUID.randomUUID()
        );

        Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "data", responseData
        );

        logger.info("<-- MOCK TRUSTLY SERVICE: Sending back successful response for transaction ID: {}", txId);
        return ResponseEntity.ok(response);
    }
}