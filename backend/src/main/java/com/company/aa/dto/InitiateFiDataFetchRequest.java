package com.company.aa.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InitiateFiDataFetchRequest(
        @NotNull UUID consentRecordId
) {
}
