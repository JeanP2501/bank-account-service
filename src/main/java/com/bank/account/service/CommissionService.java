package com.bank.account.service;

import com.bank.account.model.entity.Account;
import com.bank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service for managing account commissions and transaction limits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionService {

    private final AccountRepository accountRepository;

    /**
     * Calculate commission for a transaction.
     * Updates transaction counter and returns commission amount.
     *
     * @param accountId the account id
     * @return Mono with commission amount
     */
    public Mono<BigDecimal> calculateAndApplyCommission(String accountId) {
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        return accountRepository.findById(accountId)
                .flatMap(account -> {
                    // Get commission BEFORE incrementing
                    BigDecimal commission = account.getNextTransactionCommission();

                    // Increment counter
                    account.incrementTransactionCount(currentMonth, currentYear);

                    log.debug("Account {} - Transactions this month: {}/{}, Commission: {}",
                            accountId,
                            account.getCurrentMonthTransactionCount(),
                            account.getFreeTransactionsPerMonth(),
                            commission);

                    // Save updated account
                    return accountRepository.save(account)
                            .thenReturn(commission);
                });
    }

    /**
     * Get commission for next transaction without applying it.
     *
     * @param accountId the account id
     * @return Mono with commission amount
     */
    public Mono<BigDecimal> getNextTransactionCommission(String accountId) {
        return accountRepository.findById(accountId)
                .map(Account::getNextTransactionCommission);
    }

    /**
     * Reset monthly transaction counter.
     * Called by scheduled job or manually
     *
     * @param accountId the account id
     * @return Mono<Void>
     */
    public Mono<Void> resetMonthlyCounter(String accountId) {
        LocalDateTime now = LocalDateTime.now();

        return accountRepository.findById(accountId)
                .flatMap(account -> {
                    account.setCurrentMonthTransactionCount(0);
                    account.setLastTransactionMonth(now.getMonthValue());
                    account.setLastTransactionYear(now.getYear());

                    return accountRepository.save(account);
                })
                .then();
    }
}