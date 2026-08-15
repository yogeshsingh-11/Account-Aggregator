package com.company.aa.dto.digio;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

/**
 * Payload sent to Digio's AA consent-creation endpoint.
 * Field names are illustrative — confirm exact keys against the Digio AA API reference
 * supplied in your onboarding docs and adjust here; this is the single place that needs
 * to change if Digio's contract differs.
 */
@Builder
public record DigioCreateConsentRequest(
        @JsonProperty("customer_identifier") String customerIdentifier,
        @JsonProperty("customer_id_type") String customerIdType, // e.g. MOBILE
        @JsonProperty("template_name") String templateName,      // your sandbox template id
        @JsonProperty("purpose_code") String purposeCode,
        @JsonProperty("fi_types") List<String> fiTypes,
        @JsonProperty("data_range_from") String dataRangeFrom,   // ISO-8601
        @JsonProperty("data_range_to") String dataRangeTo,
        @JsonProperty("consent_expiry") String consentExpiry,
        @JsonProperty("redirect_url") String redirectUrl
) {
}
