package com.cafeteria.orderservice.util;

import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Decodes the JWT payload (middle segment) without signature verification.
 * Signature verification already happened at the API Gateway / user-service.
 * Used purely to extract claims like userId and roles for internal routing.
 */
@Component
public class JwtDecoder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Long extractUserId(String bearerHeader) {
        try {
            Map<?, ?> claims = extractClaims(bearerHeader);
            Object userId = claims.get("userId");
            if (userId instanceof Number n) {
                return n.longValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean hasRole(String bearerHeader, String role) {
        try {
            Map<?, ?> claims = extractClaims(bearerHeader);
            Object roles = claims.get("roles");
            if (roles != null) {
                return roles.toString().contains(role);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private Map<?, ?> extractClaims(String bearerHeader) throws Exception {
        String token = bearerHeader.startsWith("Bearer ") ? bearerHeader.substring(7) : bearerHeader;
        String payload = token.split("\\.")[1];
        byte[] decoded = Base64.getUrlDecoder().decode(payload);
        return MAPPER.readValue(decoded, Map.class);
    }
}
