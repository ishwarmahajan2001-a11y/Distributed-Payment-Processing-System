package com.elos.payments.trustly_provider_service.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "api_logs") // Specifies the name of the MongoDB collection (like a table)
@Data
public class TrustlyApiLog {
    @Id
    private String id; // MongoDB uses String IDs by default
    private String transactionReferenceId;
    private String requestPayload; // store the raw JSON string
    private String responsePayload; // store the raw JSON string
    private LocalDateTime timestamp = LocalDateTime.now();
}