package com.bank.account.client;

import com.bank.account.model.dto.CustomerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Client for communicating with Customer Service
 * Uses WebClient for reactive HTTP calls
 */
@Slf4j
@Component
public class CustomerClient {

    private final WebClient webClient;

    public CustomerClient(@Value("${customer.service.url}") String customerServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(customerServiceUrl)
                .build();
    }

    /**
     * Get customer by ID from Customer Service
     * @param customerId the customer id
     * @return Mono of CustomerResponse
     */
    public Mono<CustomerResponse> getCustomerById(String customerId) {
        log.debug("Calling Customer Service to get customer with id: {}", customerId);

        return webClient.get()
                .uri("/api/customers/{id}", customerId)
                .retrieve()
                .bodyToMono(CustomerResponse.class)
                .doOnSuccess(customer -> log.debug("Customer found: {}", customer.getId()))
                .doOnError(WebClientResponseException.class, ex -> {
                    log.error("Error calling Customer Service: {} - {}", ex.getStatusCode(), ex.getMessage());
                })
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    log.warn("Customer not found with id: {}", customerId);
                    return Mono.empty();
                });
    }

    /**
     * Check if customer exists
     * @param customerId the customer id
     * @return Mono of Boolean
     */
    public Mono<Boolean> customerExists(String customerId) {
        return getCustomerById(customerId)
                .map(customer -> true)
                .defaultIfEmpty(false);
    }
}
