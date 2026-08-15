package com.company.aa.service;

import com.company.aa.dto.digio.DigioFiDataPayload;
import com.company.aa.entity.Account;
import com.company.aa.entity.FiDataRequest;
import com.company.aa.entity.LoanAccount;
import com.company.aa.entity.Transaction;
import com.company.aa.repository.AccountRepository;
import com.company.aa.repository.LoanAccountRepository;
import com.company.aa.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Maps heterogeneous FIP payloads (as relayed by Digio) into our canonical schema.
 * This is the single place that absorbs FIP-to-FIP variability (differing date formats,
 * missing optional fields, string-vs-numeric amounts) so downstream consumers only ever
 * see the normalized model.
 */
@Service
public class DataNormalizationService {

    private static final Logger log = LoggerFactory.getLogger(DataNormalizationService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanAccountRepository loanAccountRepository;

    public DataNormalizationService(AccountRepository accountRepository,
                                     TransactionRepository transactionRepository,
                                     LoanAccountRepository loanAccountRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.loanAccountRepository = loanAccountRepository;
    }

    @Transactional
    public List<Account> normalizeAndPersist(FiDataRequest fiDataRequest, DigioFiDataPayload payload) {
        if (payload == null || payload.accounts() == null) {
            log.warn("Empty FI data payload for session {}", fiDataRequest.getSessionId());
            return List.of();
        }

        return payload.accounts().stream()
                .map(fipAccount -> normalizeAccount(fiDataRequest, fipAccount))
                .toList();
    }

    private Account normalizeAccount(FiDataRequest fiDataRequest, DigioFiDataPayload.FipAccount src) {
        Account account = Account.builder()
                .fiDataRequest(fiDataRequest)
                .fipId(safe(src.fipId()))
                .fiType(safe(src.fiType()))
                .maskedAccountNumber(safe(src.maskedAccountNumber()))
                .accountType(src.accountType())
                .ifsc(src.ifsc())
                .currency(src.currency() != null ? src.currency() : "INR")
                .currentBalance(parseAmount(src.currentBalance()))
                .balanceAsOf(parseInstant(src.balanceAsOf()))
                .status(src.status())
                .build();
        account = accountRepository.save(account);

        if (src.transactions() != null) {
            Account finalAccount = account;
            List<Transaction> txns = src.transactions().stream()
                    .map(t -> normalizeTransaction(finalAccount, t))
                    .toList();
            transactionRepository.saveAll(txns);
        }

        if (src.loanDetails() != null) {
            loanAccountRepository.save(normalizeLoan(account, src.loanDetails()));
        }

        return account;
    }

    private Transaction normalizeTransaction(Account account, DigioFiDataPayload.FipTransaction src) {
        return Transaction.builder()
                .account(account)
                .txnRefId(src.txnRefId())
                .txnType(normalizeTxnType(src.type()))
                .amount(parseAmount(src.amount()))
                .balanceAfterTxn(parseAmount(src.balanceAfterTxn()))
                .txnTimestamp(parseInstantOrNow(src.txnTimestamp()))
                .valueDate(parseLocalDate(src.valueDate()))
                .narration(truncate(src.narration(), 512))
                .mode(src.mode())
                .build();
    }

    private LoanAccount normalizeLoan(Account account, DigioFiDataPayload.FipLoanDetails src) {
        return LoanAccount.builder()
                .account(account)
                .loanType(src.loanType())
                .sanctionedAmount(parseAmount(src.sanctionedAmount()))
                .outstandingPrincipal(parseAmount(src.outstandingPrincipal()))
                .interestRate(parseAmount(src.interestRate()))
                .tenureMonths(src.tenureMonths())
                .nextEmiAmount(parseAmount(src.nextEmiAmount()))
                .nextEmiDate(parseLocalDate(src.nextEmiDate()))
                .build();
    }

    private String normalizeTxnType(String raw) {
        if (raw == null) return "DEBIT";
        String upper = raw.trim().toUpperCase();
        return (upper.startsWith("C")) ? "CREDIT" : "DEBIT";
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse amount '{}', storing null", raw);
            return null;
        }
    }

    private Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Instant.parse(raw.trim());
        } catch (Exception e) {
            log.warn("Could not parse timestamp '{}', storing null", raw);
            return null;
        }
    }

    private Instant parseInstantOrNow(String raw) {
        Instant parsed = parseInstant(raw);
        return parsed != null ? parsed : Instant.now();
    }

    private LocalDate parseLocalDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception e) {
            log.warn("Could not parse date '{}', storing null", raw);
            return null;
        }
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private String truncate(String v, int max) {
        if (v == null) return null;
        return v.length() <= max ? v : v.substring(0, max);
    }
}
