package com.elos.payments.payment_validation_service.util;

import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class HmacUtil {
    private static final String ALGORITHM = "HmacSHA256";

    public String calculateHmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    public boolean verifyHmac(String signature, String data, String secret) throws Exception {
        String calculatedSignature = calculateHmac(data, secret);
        return calculatedSignature.equals(signature);
    }
}