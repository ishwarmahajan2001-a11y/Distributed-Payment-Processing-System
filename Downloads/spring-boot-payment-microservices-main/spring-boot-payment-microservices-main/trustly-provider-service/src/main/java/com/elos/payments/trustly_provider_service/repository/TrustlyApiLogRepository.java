package com.elos.payments.trustly_provider_service.repository;

import com.elos.payments.trustly_provider_service.document.TrustlyApiLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrustlyApiLogRepository extends MongoRepository<TrustlyApiLog, String> {

}