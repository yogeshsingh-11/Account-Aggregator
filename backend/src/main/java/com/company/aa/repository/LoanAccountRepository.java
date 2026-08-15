package com.company.aa.repository;

import com.company.aa.entity.LoanAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, UUID> {
    List<LoanAccount> findByAccountId(UUID accountId);
}
