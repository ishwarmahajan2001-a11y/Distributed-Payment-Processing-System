package com.elos.payments.payment_validation_service.util; // Use your specific package name for each service

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * A utility class to handle RSA cryptographic operations (signing and verification).
 * This component centralizes the logic for generating and verifying SHA256withRSA signatures,
 * which are used for secure service-to-service communication. It also includes helper
 * methods to load PEM-formatted keys from the application's resources.
 */
@Component
public class RsaSignatureUtil {

    private static final Logger logger = LoggerFactory.getLogger(RsaSignatureUtil.class);
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * Generates a Base64 encoded digital signature for a given text payload.
     *
     * @param plainText The data to be signed.
     * @param privateKey The private key used for signing.
     * @return A Base64 encoded string representing the signature.
     * @throws Exception if signing fails.
     */
    public String generateSignature(String plainText, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance(SIGNATURE_ALGORITHM);
        privateSignature.initSign(privateKey);
        privateSignature.update(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = privateSignature.sign();
        logger.debug("Successfully generated RSA signature.");
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * Verifies a digital signature against a text payload and a public key.
     *
     * @param plainText The original, unsigned data.
     * @param signature The Base64 encoded signature to be verified.
     * @param publicKey The public key corresponding to the private key used for signing.
     * @return true if the signature is valid, false otherwise.
     */
    public boolean verifySignature(String plainText, String signature, PublicKey publicKey) {
        try {
            Signature publicSignature = Signature.getInstance(SIGNATURE_ALGORITHM);
            publicSignature.initVerify(publicKey);
            publicSignature.update(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return publicSignature.verify(signatureBytes);
        } catch (Exception e) {
            logger.error("An error occurred during signature verification. The signature will be treated as invalid.", e);
            return false;
        }
    }

    /**
     * Loads an RSA Private Key from a PEM file located in the classpath resources.
     * This method is designed to be resilient to different PEM file formats and line endings.
     *
     * @param resourcePath The path to the key file within the resources folder (e.g., "merchant_private_key.pem").
     * @return A PrivateKey object.
     * @throws Exception if the key file cannot be read or is in an invalid format.
     */
    public PrivateKey loadPrivateKey(String resourcePath) throws Exception {
        // --- START: This is the new, more robust key loading logic ---
        String keyString = new String(readKeyBytesFromResources(resourcePath), StandardCharsets.UTF_8);

        // This block is more resilient than simple string replacement.
        // It specifically finds the content between the PEM headers, ignoring extra whitespace or line endings.
        String privateKeyPEM = keyString
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\n", "") // Explicitly remove newlines (LF)
                .replaceAll("\r", ""); // Explicitly remove carriage returns (CR)

        byte[] decodedKey = Base64.getDecoder().decode(privateKeyPEM);

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            return kf.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            // Provide a much more helpful error message if parsing fails.
            logger.error("Failed to parse private key from resource: {}. Please ensure the key is a valid, unencrypted PKCS#8 PEM file.", resourcePath, e);
            throw new RuntimeException("Could not parse the private key.", e);
        }
        // --- END: New key loading logic ---
    }

    /**
     * Loads an RSA Public Key from a PEM file located in the classpath resources.
     * This logic is generally more stable, but we'll apply the same robust cleaning.
     *
     * @param resourcePath The path to the key file within the resources folder (e.g., "merchant_public_key.pem").
     * @return A PublicKey object.
     * @throws Exception if loading or parsing fails.
     */
    public PublicKey loadPublicKey(String resourcePath) throws Exception {
        String keyString = new String(readKeyBytesFromResources(resourcePath), StandardCharsets.UTF_8);

        String publicKeyPEM = keyString
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\n", "")
                .replaceAll("\r", "");

        byte[] decodedKey = Base64.getDecoder().decode(publicKeyPEM);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * A private helper method to read a resource file into a byte array.
     * This avoids code duplication and includes a null check.
     */
    private byte[] readKeyBytesFromResources(String resourcePath) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            logger.error("FATAL: Key file not found in resources path: '{}'. The application cannot start without this key.", resourcePath);
            throw new IOException("Resource not found: " + resourcePath);
        }
        return is.readAllBytes();
    }
}