package com.company.aa.controller;

import com.company.aa.dto.FiDataFetchResponse;
import com.company.aa.dto.InitiateFiDataFetchRequest;
import com.company.aa.dto.NormalizedAccountView;
import com.company.aa.service.FiDataFetchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Business-facing (FIU-internal) endpoints for initiating data fetches and reading
 * normalized results. All gated by the internal API key filter (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/internal/fi-data")
@Tag(name = "FI Data", description = "Financial data retrieval and normalized results")
public class FiDataController {

    private final FiDataFetchService fiDataFetchService;

    public FiDataController(FiDataFetchService fiDataFetchService) {
        this.fiDataFetchService = fiDataFetchService;
    }

    @Operation(summary = "Initiate an FI data fetch session for an ACTIVE consent")
    @PostMapping("/fetch")
    public ResponseEntity<FiDataFetchResponse> initiateFetch(
            @Valid @RequestBody InitiateFiDataFetchRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Requested-By", required = false) String requestedBy) {
        FiDataFetchResponse response =
                fiDataFetchService.initiateFetch(request.consentRecordId(), idempotencyKey, requestedBy);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Get normalized accounts (with balances) delivered for a fetch request")
    @GetMapping("/{fiDataRequestId}/accounts")
    public ResponseEntity<List<NormalizedAccountView>> getAccounts(@PathVariable UUID fiDataRequestId) {
        return ResponseEntity.ok(fiDataFetchService.getAccountsForRequest(fiDataRequestId));
    }

    @Operation(summary = "Manually trigger fetch+normalize for a session (fallback if webhook was missed)")
    @PostMapping("/sessions/{sessionId}/sync")
    public ResponseEntity<List<NormalizedAccountView>> manualSync(@PathVariable String sessionId) {
        var accounts = fiDataFetchService.fetchAndNormalize(sessionId);
        var views = accounts.stream()
                .map(a -> NormalizedAccountView.builder()
                        .accountId(a.getId())
                        .fipId(a.getFipId())
                        .fiType(a.getFiType())
                        .maskedAccountNumber(a.getMaskedAccountNumber())
                        .accountType(a.getAccountType())
                        .currentBalance(a.getCurrentBalance())
                        .balanceAsOf(a.getBalanceAsOf())
                        .status(a.getStatus())
                        .build())
                .toList();
        return ResponseEntity.ok(views);
    }
}
