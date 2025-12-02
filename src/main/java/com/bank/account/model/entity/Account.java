package com.bank.account.model.entity;

import com.bank.account.model.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Account entity representing bank accounts (passive products)
 * Supports savings, checking, and fixed-term accounts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
public class Account {

    @Id
    private String id;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotNull(message = "Balance is required")
    @PositiveOrZero(message = "Balance must be zero or positive")
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Monthly maintenance fee (only for CHECKING accounts)
     */
    @PositiveOrZero(message = "Maintenance fee must be zero or positive")
    @Builder.Default
    private BigDecimal maintenanceFee = BigDecimal.ZERO;

    /**
     * Maximum monthly transactions (only for SAVING accounts)
     * Null means unlimited
     */
    private Integer maxMonthlyTransactions;

    /**
     * Current month transaction count
     */
    @Builder.Default
    private Integer currentMonthTransactions = 0;

    /**
     * Specific day of month for transactions (only for FIXED_TERM accounts)
     * Value between 1-31
     */
    private Integer transactionDay;

    /**
     * Account holders (for BUSINESS accounts)
     * List of customer IDs who are holders
     */
    @Builder.Default
    private List<String> holders = new ArrayList<>();

    /**
     * Authorized signers (for BUSINESS accounts)
     * List of customer IDs who can sign
     */
    @Builder.Default
    private List<String> authorizedSigners = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    /**
     * Check if account is a business account (has multiple holders or signers)
     */
    public boolean isBusinessAccount() {
        return !holders.isEmpty() || !authorizedSigners.isEmpty();
    }

    /**
     * Check if account has reached transaction limit
     */
    public boolean hasReachedTransactionLimit() {
        if (maxMonthlyTransactions == null) {
            return false;
        }
        return currentMonthTransactions >= maxMonthlyTransactions;
    }
}
