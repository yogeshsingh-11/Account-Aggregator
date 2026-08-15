package com.company.aa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;

import java.time.Instant;
import java.util.List;

/**
 * Business-facing request to initiate a consent flow for a customer.
 * customerRef must be an internal reference (e.g. loan application id / customer UUID) — never a raw PAN/Aadhaar.
 */
public record CreateConsentRequest(
        @NotBlank String customerRef,
        @NotBlank String purposeCode,
        @NotEmpty List<@NotBlank String> fiTypes,
        @NotNull Instant dataRangeFrom,
        @NotNull @Future Instant dataRangeTo,
        Instant consentExpiry
) {
}
