package com.company.aa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Simple API-key gate for business-facing (FIU-internal) endpoints. Swap for OAuth2/mTLS
 * between real internal services; this is intentionally minimal for the sandbox/dev phase.
 * Customer-facing endpoints used directly by the Angular UI (consent status polling, webhook
 * receiver) are exempted — they're locked down by consent-handle/session-id possession and,
 * for the webhook, by signature verification instead.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";

    private final String expectedKey;

    public ApiKeyAuthFilter(String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isInternalEndpoint = path.startsWith("/api/v1/internal/");

        if (isInternalEndpoint) {
            String provided = request.getHeader(HEADER);
            if (expectedKey == null || expectedKey.isBlank() || provided == null || !provided.equals(expectedKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid API key\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
