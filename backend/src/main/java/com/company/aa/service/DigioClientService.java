package com.company.aa.service;

import com.company.aa.config.DigioProperties;
import com.company.aa.dto.digio.DigioConsentStatusResponse;
import com.company.aa.dto.digio.DigioCreateConsentRequest;
import com.company.aa.dto.digio.DigioCreateConsentResponse;
import com.company.aa.dto.digio.DigioFiDataPayload;
import com.company.aa.dto.digio.DigioFiRequestResponse;
import com.company.aa.exception.DigioClientException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin client around Digio's Account Aggregator sandbox/production APIs.
 *
 * IMPORTANT: the exact endpoint paths below (/v1/aa/consent, etc.) are placeholders —
 * confirm the precise paths, verbs and payload shapes against the Digio AA API reference
 * from your onboarding docs, then adjust the `restTemplate.exchange(...)` calls here.
 * Every other layer of this service (controllers, ConsentService, normalization) talks only
 * to this class, so a contract change is a one-file fix.
 */
@Service
public class DigioClientService {

    private static final Logger log = LoggerFactory.getLogger(DigioClientService.class);

    private final RestTemplate digioRestTemplate;
    private final DigioProperties digioProperties;

    public DigioClientService(RestTemplate digioRestTemplate, DigioProperties digioProperties) {
        this.digioRestTemplate = digioRestTemplate;
        this.digioProperties = digioProperties;
    }

    @Retry(name = "digioClient")
    @CircuitBreaker(name = "digioClient")
    public DigioCreateConsentResponse createConsent(DigioCreateConsentRequest request) {
        try {
            var response = digioRestTemplate.postForEntity("/v1/aa/consent", request, DigioCreateConsentResponse.class);
            requireBody(response.getBody(), "createConsent");
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw translateClientError(ex, "createConsent");
        }
    }

    @Retry(name = "digioClient")
    @CircuitBreaker(name = "digioClient")
    public DigioConsentStatusResponse getConsentStatus(String consentHandle) {
        try {
            var response = digioRestTemplate.getForEntity(
                    "/v1/aa/consent/{handle}/status", DigioConsentStatusResponse.class, consentHandle);
            requireBody(response.getBody(), "getConsentStatus");
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw translateClientError(ex, "getConsentStatus");
        }
    }

    @Retry(name = "digioClient")
    @CircuitBreaker(name = "digioClient")
    public DigioFiRequestResponse initiateFiDataFetch(String consentId) {
        try {
            var response = digioRestTemplate.postForEntity(
                    "/v1/aa/fi-request", new FiRequestBody(consentId, digioProperties.templateId()),
                    DigioFiRequestResponse.class);
            requireBody(response.getBody(), "initiateFiDataFetch");
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw translateClientError(ex, "initiateFiDataFetch");
        }
    }

    @Retry(name = "digioClient")
    @CircuitBreaker(name = "digioClient")
    public DigioFiDataPayload fetchFiData(String sessionId) {
        try {
            var response = digioRestTemplate.getForEntity(
                    "/v1/aa/fi-request/{sessionId}/data", DigioFiDataPayload.class, sessionId);
            requireBody(response.getBody(), "fetchFiData");
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            throw translateClientError(ex, "fetchFiData");
        }
    }

    private void requireBody(Object body, String op) {
        if (body == null) {
            throw new DigioClientException("Empty response body from Digio for operation: " + op);
        }
    }

    private DigioClientException translateClientError(HttpClientErrorException ex, String op) {
        HttpStatusCode status = ex.getStatusCode();
        log.error("Digio {} failed with status {}: {}", op, status, ex.getResponseBodyAsString());
        // 4xx are business/validation errors from Digio — not retried (see application.yml ignore-exceptions).
        return new DigioClientException("Digio " + op + " failed: HTTP " + status.value(), ex);
    }

    private record FiRequestBody(String consentId, String templateId) {
    }
}
