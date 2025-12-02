package com.bank.account.exception;

/**
 * Exception thrown when an account is not found
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String id) {
        super("Account not found with id: " + id);
    }

    public AccountNotFoundException(String field, String value) {
        super(String.format("Account not found with %s: %s", field, value));
    }
}
