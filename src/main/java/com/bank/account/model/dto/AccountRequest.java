package com.bank.account.model.dto;

import com.bank.account.model.enums.AccountType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for account creation and update requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @PositiveOrZero(message = "Initial balance must be zero or positive")
    private BigDecimal initialBalance;

    @PositiveOrZero(message = "Maintenance fee must be zero or positive")
    private BigDecimal maintenanceFee;

    @Min(value = 1, message = "Max monthly transactions must be at least 1")
    private Integer maxMonthlyTransactions;

    @Min(value = 1, message = "Transaction day must be between 1 and 31")
    @Max(value = 31, message = "Transaction day must be between 1 and 31")
    private Integer transactionDay;

    private List<String> holders;

    private List<String> authorizedSigners;

    /**
     * Minimum opening amount (optional, default 0).
     */
    private BigDecimal minimumOpeningAmount;

    /**
     * Free transactions per month (optional, default 5).
     */
    private Integer freeTransactionsPerMonth;

    /**
     * Commission per transaction (optional, default 2.00).
     */
    private BigDecimal commissionPerTransaction;

    /**
     * Minimum daily average balance for VIP accounts (optional).
     */
    private BigDecimal minimumDailyAverage;
}
