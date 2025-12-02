package com.bank.account.repository;

import com.bank.account.model.entity.Account;
import com.bank.account.model.enums.AccountType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for Account entity
 * Provides CRUD operations and custom queries
 */
@Repository
public interface AccountRepository extends ReactiveMongoRepository<Account, String> {

    /**
     * Find account by account number
     * @param accountNumber the account number
     * @return Mono of Account
     */
    Mono<Account> findByAccountNumber(String accountNumber);

    /**
     * Find all accounts by customer ID
     * @param customerId the customer id
     * @return Flux of Accounts
     */
    Flux<Account> findByCustomerId(String customerId);

    /**
     * Find accounts by customer ID and account type
     * @param customerId the customer id
     * @param accountType the account type
     * @return Flux of Accounts
     */
    Flux<Account> findByCustomerIdAndAccountType(String customerId, AccountType accountType);

    /**
     * Check if account exists by account number
     * @param accountNumber the account number
     * @return Mono of Boolean
     */
    Mono<Boolean> existsByAccountNumber(String accountNumber);

    /**
     * Count accounts by customer ID and account type
     * @param customerId the customer id
     * @param accountType the account type
     * @return Mono of Long
     */
    Mono<Long> countByCustomerIdAndAccountType(String customerId, AccountType accountType);
}
