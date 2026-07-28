package com.elos.payments.payment_validation_service.filter;

import com.elos.payments.payment_validation_service.util.HmacUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A Spring Security Filter that intercepts incoming requests to validate the HMAC signature.
 * This ensures that only trusted clients possessing the shared secret key can communicate with our public API.
 * It checks for the 'X-Client-Signature' header and verifies its authenticity against the request body.
 * It also includes logic to bypass this check for specific, non-sensitive administrative endpoints.
 */
@Component
public class HmacFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(HmacFilter.class);
    private static final String HMAC_HEADER = "X-Client-Signature";

    // A list of paths that should not be subjected to HMAC validation.
    private static final List<String> EXCLUDED_PATHS = List.of("/api/validate/rules/refresh");

    @Autowired
    private HmacUtil hmacUtil;

    @Value("${client.hmac.secret}")
    private String hmacSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // First, check if the request path is in our exclusion list.
        // This is done before any other processing to efficiently bypass security for known endpoints.
        if (EXCLUDED_PATHS.contains(request.getRequestURI())) {
            logger.debug("Skipping HMAC validation for excluded administrative endpoint: {}", request.getRequestURI());
            filterChain.doFilter(request, response); // Pass the original request straight to the controller.
            return; // And stop the filter's execution here.
        }

        // For all other requests, we proceed with HMAC validation.
        // We wrap the request to allow its body to be read multiple times (once here, once in the controller).
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);

        String clientSignature = requestWrapper.getHeader(HMAC_HEADER);
        if (clientSignature == null || clientSignature.isBlank()) {
            logger.warn("Request rejected: Missing {} header from IP: {}", HMAC_HEADER, request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Authorization header is missing.");
            return;
        }

        try {
            // Read the raw request body to a string. This is the data we must use for verification.
            String requestBody = new String(requestWrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // The core security check: verify the client's signature against our own calculation.
            if (!hmacUtil.verifyHmac(clientSignature, requestBody, hmacSecret)) {
                // This is a critical security event. We log it as an error for monitoring.
                logger.error("Request rejected: Invalid HMAC signature. Client IP: {}", request.getRemoteAddr());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Invalid signature.");
                return;
            }

        } catch (Exception e) {
            // If anything goes wrong during the cryptographic process, it's a server-side issue.
            logger.error("An unexpected exception occurred during HMAC verification.", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().write("Error during signature verification.");
            return;
        }

        // If we've reached this point, the signature is valid.
        logger.debug("HMAC signature verified successfully for {}. Proceeding with request chain.", request.getRequestURI());
        // Pass the wrapped request (which still contains the body) along to the next filter or controller.
        filterChain.doFilter(requestWrapper, response);
    }
}