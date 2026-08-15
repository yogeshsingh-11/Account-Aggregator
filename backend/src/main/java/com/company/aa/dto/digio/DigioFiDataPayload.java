package com.company.aa.dto.digio;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Shape of the decrypted FI data payload as delivered by the AA ecosystem (FIP -> AA -> FIU),
 * following the standard ReBIT/Sahamati FI data XML-to-JSON structure that Digio exposes.
 * Confirm precise field names against the Digio "Fetch FI Data" API reference — this class
 * is intentionally the single translation point between the wire format and our canonical model.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DigioFiDataPayload(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("accounts") List<FipAccount> accounts
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FipAccount(
            @JsonProperty("fip_id") String fipId,
            @JsonProperty("fi_type") String fiType,
            @JsonProperty("masked_account_number") String maskedAccountNumber,
            @JsonProperty("account_type") String accountType,
            @JsonProperty("ifsc") String ifsc,
            @JsonProperty("currency") String currency,
            @JsonProperty("current_balance") String currentBalance,
            @JsonProperty("balance_as_of") String balanceAsOf,
            @JsonProperty("status") String status,
            @JsonProperty("transactions") List<FipTransaction> transactions,
            @JsonProperty("loan_details") FipLoanDetails loanDetails
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FipTransaction(
            @JsonProperty("txn_ref_id") String txnRefId,
            @JsonProperty("type") String type, // DEBIT / CREDIT
            @JsonProperty("amount") String amount,
            @JsonProperty("current_balance") String balanceAfterTxn,
            @JsonProperty("transaction_timestamp") String txnTimestamp,
            @JsonProperty("value_date") String valueDate,
            @JsonProperty("narration") String narration,
            @JsonProperty("mode") String mode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FipLoanDetails(
            @JsonProperty("loan_type") String loanType,
            @JsonProperty("sanctioned_amount") String sanctionedAmount,
            @JsonProperty("outstanding_principal") String outstandingPrincipal,
            @JsonProperty("interest_rate") String interestRate,
            @JsonProperty("tenure_months") Integer tenureMonths,
            @JsonProperty("next_emi_amount") String nextEmiAmount,
            @JsonProperty("next_emi_date") String nextEmiDate
    ) {
    }
}
