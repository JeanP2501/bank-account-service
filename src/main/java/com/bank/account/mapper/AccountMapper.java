package com.bank.account.mapper;

import com.bank.account.model.dto.AccountRequest;
import com.bank.account.model.dto.AccountResponse;
import com.bank.account.model.entity.Account;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper for converting between Account entity and DTOs
 */
@Component
public class AccountMapper {

    /**
     * Convert AccountRequest to Account entity
     * @param request the account request
     * @return Account entity
     */
    public Account toEntity(AccountRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .accountType(request.getAccountType())
                .customerId(request.getCustomerId())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : java.math.BigDecimal.ZERO)
                .maintenanceFee(request.getMaintenanceFee())
                .maxMonthlyTransactions(request.getMaxMonthlyTransactions())
                .currentMonthTransactions(0)
                .transactionDay(request.getTransactionDay())
                .holders(request.getHolders() != null ? request.getHolders() : new java.util.ArrayList<>())
                .authorizedSigners(request.getAuthorizedSigners() != null ? request.getAuthorizedSigners() : new java.util.ArrayList<>())
                .minimumOpeningAmount(request.getMinimumOpeningAmount() != null
                        ? request.getMinimumOpeningAmount()
                        : BigDecimal.ZERO)
                .freeTransactionsPerMonth(request.getFreeTransactionsPerMonth() != null
                        ? request.getFreeTransactionsPerMonth()
                        : 5)
                .commissionPerTransaction(request.getCommissionPerTransaction() != null
                        ? request.getCommissionPerTransaction()
                        : new BigDecimal("2.00"))
                .currentMonthTransactionCount(0)
                .lastTransactionMonth(now.getMonthValue())
                .lastTransactionYear(now.getYear())
                .minimumDailyAverage(request.getMinimumDailyAverage())
                .createdAt(now)
                .build();

        configureAccountByType(account);

        return account;
    }

    /**
     * Convert Account entity to AccountResponse
     * @param account the account entity
     * @return AccountResponse DTO
     */
    public AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .customerId(account.getCustomerId())
                .balance(account.getBalance())
                .maintenanceFee(account.getMaintenanceFee())
                .maxMonthlyTransactions(account.getMaxMonthlyTransactions())
                .currentMonthTransactions(account.getCurrentMonthTransactions())
                .transactionDay(account.getTransactionDay())
                .holders(account.getHolders())
                .authorizedSigners(account.getAuthorizedSigners())
                .minimumOpeningAmount(account.getMinimumOpeningAmount())
                .freeTransactionsPerMonth(account.getFreeTransactionsPerMonth())
                .commissionPerTransaction(account.getCommissionPerTransaction())
                .currentMonthTransactionCount(account.getCurrentMonthTransactionCount())
                .nextTransactionCommission(account.getNextTransactionCommission())
                .minimumDailyAverage(account.getMinimumDailyAverage())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    /**
     * Update existing Account entity with request data
     * @param account the existing account
     * @param request the update request
     */
    public void updateEntity(Account account, AccountRequest request) {
        account.setMaintenanceFee(request.getMaintenanceFee());
        account.setMaxMonthlyTransactions(request.getMaxMonthlyTransactions());
        account.setTransactionDay(request.getTransactionDay());
        account.setHolders(request.getHolders() != null ? request.getHolders() : account.getHolders());
        account.setAuthorizedSigners(request.getAuthorizedSigners() != null ? request.getAuthorizedSigners() : account.getAuthorizedSigners());
        account.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Generate unique account number
     * @return account number
     */
    private String generateAccountNumber() {
        // Format: ACC-XXXXXXXXXX (10 random digits)
        return "ACC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    /**
     * Configure account-specific fields based on type.
     * @param account the account to configure
     */
    private void configureAccountByType(Account account) {
        switch (account.getAccountType()) {
            case SAVING:
                // Standard saving account
                break;
            case CHECKING:
                account.setMaintenanceFee(new BigDecimal("10.00"));
                break;
            case FIXED_TERM:
                account.setTransactionDay(1); // Monthly on day 1
                break;
        }
    }

}
