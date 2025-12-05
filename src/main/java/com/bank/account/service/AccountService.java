package com.bank.account.service;

import com.bank.account.client.CustomerClient;
import com.bank.account.exception.AccountNotFoundException;
import com.bank.account.exception.BusinessRuleException;
import com.bank.account.exception.CustomerNotFoundException;
import com.bank.account.mapper.AccountMapper;
import com.bank.account.model.dto.AccountRequest;
import com.bank.account.model.dto.AccountResponse;
import com.bank.account.model.dto.CustomerResponse;
import com.bank.account.model.dto.EntityActionEvent;
import com.bank.account.model.entity.Account;
import com.bank.account.model.enums.AccountType;
import com.bank.account.model.enums.CustomerType;
import com.bank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service layer for Account operations
 * Implements business logic and rules for account management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final CustomerClient customerClient;
    private final CommissionService commissionService;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Create a new account with business rule validations
     * @param request the account request
     * @return Mono of AccountResponse
     */
    public Mono<AccountResponse> create(AccountRequest request) {
        log.debug("Creating account for customer: {}", request.getCustomerId());

        return customerClient.getCustomerById(request.getCustomerId())
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(request.getCustomerId())))
                .flatMap(customer -> {
                    Mono<AccountRequest> accountValidated = validateBusinessRules(request, customer);
                    // Validate initial balance meets minimum
                    BigDecimal initialBalance = request.getInitialBalance() != null
                            ? request.getInitialBalance()
                            : BigDecimal.ZERO;

                    BigDecimal minimumOpening = request.getMinimumOpeningAmount() != null
                            ? request.getMinimumOpeningAmount()
                            : BigDecimal.ZERO;

                    if (initialBalance.compareTo(minimumOpening) < 0) {
                        return Mono.error(new BusinessRuleException(
                                String.format("Initial balance %.2f is less than minimum opening amount %.2f",
                                        initialBalance, minimumOpening)));
                    }
                    validateAccountRules(customer, request)
                        .then(Mono.defer(() -> {
                            Account account = accountMapper.toEntity(request);
                            return accountRepository.save(account)
                                    .flatMap(savedCustomer -> {
                                        // Publicar evento después de guardar exitosamente
                                        EntityActionEvent event = EntityActionEvent.builder()
                                                .eventId(UUID.randomUUID().toString())
                                                .eventType("ACCOUNT_CREATED")
                                                .entityType(account.getClass().getSimpleName())
                                                .payload(account)
                                                .timestamp(LocalDateTime.now())
                                                .build();
                                        return kafkaProducerService.sendEvent(savedCustomer.getId(), event)
                                                .doOnSuccess(v -> log.info("Customer created and event published: {}", account.getId()))
                                                .doOnError(e -> log.error("Error publishing event: {}", e.getMessage()))
                                                .thenReturn(savedCustomer);
                                    });
                        }));

                    return accountValidated;
                })
                .map(accountMapper::toEntity)
                .flatMap(account -> applyAccountTypeDefaults(account))
                .flatMap(accountRepository::save)
                .flatMap(savedAccount -> {
                    // Publicar evento después de guardar exitosamente
                    EntityActionEvent event = EntityActionEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("ACCOUNT_CREATED")
                            .entityType(savedAccount.getClass().getSimpleName())
                            .payload(savedAccount)
                            .timestamp(LocalDateTime.now())
                            .build();
                    return kafkaProducerService.sendEvent(savedAccount.getId(), event)
                            .doOnSuccess(v -> log.info("Customer created and event published: {}", savedAccount.getId()))
                            .doOnError(e -> log.error("Error publishing event: {}", e.getMessage()))
                            .thenReturn(savedAccount);
                })
                .doOnSuccess(account -> log.info("Account created successfully: {}", account.getAccountNumber()))
                .map(accountMapper::toResponse);
    }

    /**
     * Validate business rules for account creation
     */
    private Mono<AccountRequest> validateBusinessRules(AccountRequest request, CustomerResponse customer) {
        log.debug("Validating business rules for customer type: {}", customer.getCustomerType());

        if (customer.getCustomerType() == CustomerType.PERSONAL) {
            return validatePersonalCustomerRules(request, customer);
        } else {
            return validateBusinessCustomerRules(request, customer);
        }
    }

    /**
     * Validate account creation rules based on customer type.
     * @param customer the customer
     * @param request the account request
     * @return Mono<Void> empty if valid, error if invalid
     */
    private Mono<Void> validateAccountRules(CustomerResponse customer, AccountRequest request) {
        // VIP customers creating saving accounts need credit card
        if (CustomerType.PERSONAL_VIP == customer.getCustomerType() &&
            AccountType.SAVING == request.getAccountType()) {

            if (!Boolean.TRUE.equals(customer.getHasCreditCard())) {
                return Mono.error(new BusinessRuleException(
                        "VIP saving accounts require an active credit card"));
            }
        }

        // PYME customers creating checking accounts need credit card
        if (CustomerType.BUSINESS_PYME == customer.getCustomerType() &&
            AccountType.CHECKING == request.getAccountType()) {

            if (!Boolean.TRUE.equals(customer.getHasCreditCard())) {
                return Mono.error(new BusinessRuleException(
                        "PYME checking accounts require an active credit card"));
            }
        }

        return Mono.empty();
    }

    /**
     * Validate rules for PERSONAL customers
     * - Can have maximum one account of each type (SAVING, CHECKING, FIXED_TERM)
     */
    private Mono<AccountRequest> validatePersonalCustomerRules(AccountRequest request, CustomerResponse customer) {
        return accountRepository.countByCustomerIdAndAccountType(customer.getId(), request.getAccountType())
                .flatMap(count -> {
                    if (count > 0) {
                        return Mono.error(new BusinessRuleException(
                                String.format("Personal customer can only have one %s account", request.getAccountType())));
                    }

                    // Personal customers cannot have holders or signers
                    if ((request.getHolders() != null && !request.getHolders().isEmpty()) ||
                            (request.getAuthorizedSigners() != null && !request.getAuthorizedSigners().isEmpty())) {
                        return Mono.error(new BusinessRuleException(
                                "Personal customer accounts cannot have additional holders or signers"));
                    }

                    return Mono.just(request);
                });
    }

    /**
     * Validate rules for BUSINESS customers
     * - Cannot have SAVING or FIXED_TERM accounts
     * - Can have multiple CHECKING accounts
     */
    private Mono<AccountRequest> validateBusinessCustomerRules(AccountRequest request, CustomerResponse customer) {
        if (request.getAccountType() == AccountType.SAVING || request.getAccountType() == AccountType.FIXED_TERM) {
            return Mono.error(new BusinessRuleException(
                    "Business customers can only have CHECKING accounts"));
        }

        return Mono.just(request);
    }

    /**
     * Apply default values based on account type
     */
    private Mono<Account> applyAccountTypeDefaults(Account account) {
        switch (account.getAccountType()) {
            case SAVING:
                // Savings: no fee, limited transactions (default 5 per month)
                if (account.getMaxMonthlyTransactions() == null) {
                    account.setMaxMonthlyTransactions(5);
                }
                account.setMaintenanceFee(java.math.BigDecimal.ZERO);
                break;

            case CHECKING:
                // Checking: has maintenance fee (default 10), unlimited transactions
                if (account.getMaintenanceFee() == null) {
                    account.setMaintenanceFee(new java.math.BigDecimal("10.00"));
                }
                account.setMaxMonthlyTransactions(null); // Unlimited
                break;

            case FIXED_TERM:
                // Fixed term: no fee, one transaction per month on specific day
                account.setMaintenanceFee(java.math.BigDecimal.ZERO);
                if (account.getTransactionDay() == null) {
                    account.setTransactionDay(1); // Default to first day of month
                }
                break;
        }

        return Mono.just(account);
    }

    /**
     * Find all accounts
     * @return Flux of AccountResponse
     */
    public Flux<AccountResponse> findAll() {
        log.debug("Finding all accounts");
        return accountRepository.findAll()
                .map(accountMapper::toResponse)
                .doOnComplete(() -> log.debug("Retrieved all accounts"));
    }

    /**
     * Find account by ID
     * @param id the account id
     * @return Mono of AccountResponse
     */
    public Mono<AccountResponse> findById(String id) {
        log.debug("Finding account by id: {}", id);
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(id)))
                .map(accountMapper::toResponse)
                .doOnSuccess(account -> log.debug("Account found with id: {}", id));
    }

    /**
     * Find account by account number
     * @param accountNumber the account number
     * @return Mono of AccountResponse
     */
    public Mono<AccountResponse> findByAccountNumber(String accountNumber) {
        log.debug("Finding account by account number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("accountNumber", accountNumber)))
                .map(accountMapper::toResponse)
                .doOnSuccess(account -> log.debug("Account found with number: {}", accountNumber));
    }

    /**
     * Find all accounts by customer ID
     * @param customerId the customer id
     * @return Flux of AccountResponse
     */
    public Flux<AccountResponse> findByCustomerId(String customerId) {
        log.debug("Finding accounts for customer: {}", customerId);
        return accountRepository.findByCustomerId(customerId)
                .map(accountMapper::toResponse)
                .doOnComplete(() -> log.debug("Retrieved accounts for customer: {}", customerId));
    }

    /**
     * Update account
     * @param id the account id
     * @param request the account request
     * @return Mono of AccountResponse
     */
    public Mono<AccountResponse> update(String id, AccountRequest request) {
        log.debug("Updating account with id: {}", id);

        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(id)))
                .flatMap(existingAccount -> {
                    accountMapper.updateEntity(existingAccount, request);
                    return accountRepository.save(existingAccount);
                })
                .flatMap(updatedAccount -> {
                    // Publicar evento después de actualizar exitosamente
                    EntityActionEvent event = EntityActionEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("ACCOUNT_UPDATED")
                            .entityType(updatedAccount.getClass().getSimpleName())
                            .timestamp(LocalDateTime.now())
                            .payload(updatedAccount)
                            .build();
                    return kafkaProducerService.sendEvent(updatedAccount.getId(), event)
                            .doOnSuccess(v -> log.info("Customer updated and event published: {}", id))
                            .doOnError(e -> log.error("Error publishing event: {}", e.getMessage()))
                            .thenReturn(updatedAccount);
                })
                .doOnSuccess(account -> log.info("Account updated successfully with id: {}", id))
                .map(accountMapper::toResponse);
    }

    /**
     * Delete account by ID
     * @param id the account id
     * @return Mono of Void
     */
    public Mono<Void> delete(String id) {
        log.debug("Deleting account with id: {}", id);

        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(id)))
                .flatMap(account -> accountRepository.deleteById(id)
                        .then(Mono.defer(() -> {
                                    // Publicar evento después de eliminar exitosamente
                                    EntityActionEvent event = EntityActionEvent.builder()
                                            .eventId(UUID.randomUUID().toString())
                                            .eventType("CUSTOMER_DELETED")
                                            .entityType(account.getClass().getSimpleName())
                                            .timestamp(LocalDateTime.now())
                                            .payload(account)
                                            .build();
                                    return kafkaProducerService.sendEvent(id, event)
                                            .doOnSuccess(v -> log.info("Account deleted successfully with id: {}", id))
                                            .doOnError(e -> log.error("Error publishing event: {}", e.getMessage()));
                        })));
    }

    public Mono<Map<String, Object>> getNextTransactionComission(String id) {
        return accountRepository.findById(id)
                .map(account -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("accountId", id);
                    response.put("currentMonthTransactions",
                            account.getCurrentMonthTransactionCount());
                    response.put("freeTransactionsPerMonth",
                            account.getFreeTransactionsPerMonth());
                    response.put("nextTransactionCommission",
                            account.getNextTransactionCommission());
                    response.put("hasFreeTransactionsAvailable",
                            account.hasFreeTransactionsAvailable());
                    return response;
                });
    }

    public Mono<Map<String, Object>> calculateAndApplyComission(String id) {
        return commissionService.calculateAndApplyCommission(id)
                .flatMap(commission ->
                        accountRepository.findById(id)
                                .map(account -> {
                                    Map<String, Object> response = new HashMap<>();
                                    response.put("accountId", id);
                                    response.put("commission", commission);
                                    response.put("currentMonthTransactions",
                                            account.getCurrentMonthTransactionCount());
                                    response.put("freeTransactionsPerMonth",
                                            account.getFreeTransactionsPerMonth());
                                    return response;
                                })
                );
    }

    public Mono<String> getDelayedResult() {
        return customerClient.getDelayedResult();
    }
}