package com.company.aa.service;

import com.company.aa.dto.digio.DigioFiDataPayload;
import com.company.aa.entity.Account;
import com.company.aa.entity.FiDataRequest;
import com.company.aa.repository.AccountRepository;
import com.company.aa.repository.LoanAccountRepository;
import com.company.aa.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataNormalizationServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private LoanAccountRepository loanAccountRepository;
    private DataNormalizationService service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        loanAccountRepository = mock(LoanAccountRepository.class);
        service = new DataNormalizationService(accountRepository, transactionRepository, loanAccountRepository);

        // echo back whatever is saved, assigning a fake id via the builder default
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void normalizesAccountAndTransactionsFromFipPayload() {
        FiDataRequest fiDataRequest = FiDataRequest.builder().build();

        var txn = new DigioFiDataPayload.FipTransaction(
                "TXN123", "credit", "1500.50", "20500.75",
                "2026-08-01T10:15:30Z", "2026-08-01", "Salary credit", "NEFT");

        var fipAccount = new DigioFiDataPayload.FipAccount(
                "FIP001", "DEPOSIT", "XXXXXXXX1234", "SAVINGS", "HDFC0001234",
                "INR", "20500.75", "2026-08-01T23:59:59Z", "ACTIVE",
                List.of(txn), null);

        var payload = new DigioFiDataPayload("session-abc", List.of(fipAccount));

        List<Account> result = service.normalizeAndPersist(fiDataRequest, payload);

        assertEquals(1, result.size());
        Account account = result.get(0);
        assertEquals("FIP001", account.getFipId());
        assertEquals("XXXXXXXX1234", account.getMaskedAccountNumber());
        assertEquals(new BigDecimal("20500.75"), account.getCurrentBalance());

        ArgumentCaptor<List<com.company.aa.entity.Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        var savedTxns = captor.getValue();
        assertEquals(1, savedTxns.size());
        assertEquals("CREDIT", savedTxns.get(0).getTxnType());
        assertEquals(new BigDecimal("1500.50"), savedTxns.get(0).getAmount());
    }

    @Test
    void handlesUnparseableAmountsGracefullyWithoutThrowing() {
        FiDataRequest fiDataRequest = FiDataRequest.builder().build();

        var fipAccount = new DigioFiDataPayload.FipAccount(
                "FIP002", "DEPOSIT", "XXXXXXXX9999", "SAVINGS", null,
                "INR", "not-a-number", "bad-date", "ACTIVE", List.of(), null);

        var payload = new DigioFiDataPayload("session-xyz", List.of(fipAccount));

        List<Account> result = service.normalizeAndPersist(fiDataRequest, payload);

        assertEquals(1, result.size());
        assertNull(result.get(0).getCurrentBalance());
        assertNull(result.get(0).getBalanceAsOf());
    }

    @Test
    void emptyPayloadReturnsEmptyList() {
        FiDataRequest fiDataRequest = FiDataRequest.builder().build();
        assertTrue(service.normalizeAndPersist(fiDataRequest, null).isEmpty());
    }
}
