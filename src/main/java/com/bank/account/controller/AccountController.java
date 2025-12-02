package com.bank.account.controller;

import com.bank.account.model.dto.AccountRequest;
import com.bank.account.model.dto.AccountResponse;
import com.bank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller for Account operations
 * Provides endpoints for CRUD operations on bank accounts
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Create a new account
     * POST /api/accounts
     * @param request the account request
     * @return Mono of AccountResponse with 201 status
     */
    @PostMapping
    public Mono<ResponseEntity<AccountResponse>> create(@Valid @RequestBody AccountRequest request) {
        log.info("POST /api/accounts - Creating account for customer: {}", request.getCustomerId());
        return accountService.create(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    /**
     * Get all accounts
     * GET /api/accounts
     * @return Flux of AccountResponse with 200 status
     */
    @GetMapping
    public Mono<ResponseEntity<Flux<AccountResponse>>> findAll() {
        log.info("GET /api/accounts - Fetching all accounts");
        return Mono.just(ResponseEntity.ok(accountService.findAll()));
    }

    /**
     * Get account by ID
     * GET /api/accounts/{id}
     * @param id the account id
     * @return Mono of AccountResponse with 200 status
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<AccountResponse>> findById(@PathVariable String id) {
        log.info("GET /api/accounts/{} - Fetching account by id", id);
        return accountService.findById(id)
                .map(ResponseEntity::ok);
    }

    /**
     * Get account by account number
     * GET /api/accounts/number/{accountNumber}
     * @param accountNumber the account number
     * @return Mono of AccountResponse with 200 status
     */
    @GetMapping("/number/{accountNumber}")
    public Mono<ResponseEntity<AccountResponse>> findByAccountNumber(@PathVariable String accountNumber) {
        log.info("GET /api/accounts/number/{} - Fetching account by number", accountNumber);
        return accountService.findByAccountNumber(accountNumber)
                .map(ResponseEntity::ok);
    }

    /**
     * Get all accounts by customer ID
     * GET /api/accounts/customer/{customerId}
     * @param customerId the customer id
     * @return Flux of AccountResponse with 200 status
     */
    @GetMapping("/customer/{customerId}")
    public Mono<ResponseEntity<Flux<AccountResponse>>> findByCustomerId(@PathVariable String customerId) {
        log.info("GET /api/accounts/customer/{} - Fetching accounts for customer", customerId);
        return Mono.just(ResponseEntity.ok(accountService.findByCustomerId(customerId)));
    }

    /**
     * Update account
     * PUT /api/accounts/{id}
     * @param id the account id
     * @param request the account request
     * @return Mono of AccountResponse with 200 status
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<AccountResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody AccountRequest request) {
        log.info("PUT /api/accounts/{} - Updating account", id);
        return accountService.update(id, request)
                .map(ResponseEntity::ok);
    }

    /**
     * Delete account
     * DELETE /api/accounts/{id}
     * @param id the account id
     * @return Mono of Void with 204 status
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        log.info("DELETE /api/accounts/{} - Deleting account", id);
        return accountService.delete(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}
