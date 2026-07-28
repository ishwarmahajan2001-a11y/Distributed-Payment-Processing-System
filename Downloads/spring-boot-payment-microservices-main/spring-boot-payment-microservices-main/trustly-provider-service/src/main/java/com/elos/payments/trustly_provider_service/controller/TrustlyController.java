package com.elos.payments.trustly_provider_service.controller;

// --- MODIFICATION: Use the FULL, correct package path for all imports ---
import com.elos.payments.trustly_provider_service.dto.ProviderRequestDTO;
import com.elos.payments.trustly_provider_service.service.TrustlyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider/trustly")
public class TrustlyController {

    @Autowired
    private TrustlyService trustlyService;

    @PostMapping("/initiate")
    public ResponseEntity<String> initiatePayment(@RequestBody ProviderRequestDTO request) throws Exception {

        String trustlyResponse = trustlyService.processTrustlyPayment(request);

        // We pass the raw JSON response from Trustly back upstream.
        return ResponseEntity.ok(trustlyResponse);
    }
}