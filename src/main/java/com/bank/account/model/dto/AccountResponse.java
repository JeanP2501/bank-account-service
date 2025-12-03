package com.bank.account.model.dto;

import com.bank.account.model.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for account response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private String id;
    private String accountNumber;
    private AccountType accountType;
    private String customerId;
    private BigDecimal balance;
    private BigDecimal maintenanceFee;
    private Integer maxMonthlyTransactions;
    private Integer currentMonthTransactions;
    private Integer transactionDay;
    private List<String> holders;
    private List<String> authorizedSigners;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /**
     * Minimum opening amount.
     */
    private BigDecimal minimumOpeningAmount;

    /**
     * Free transactions per month.
     */
    private Integer freeTransactionsPerMonth;

    /**
     * Commission per transaction after free limit.
     */
    private BigDecimal commissionPerTransaction;

    /**
     * Current month transaction count.
     */
    private Integer currentMonthTransactionCount;

    /**
     * Next transaction commission.
     */
    private BigDecimal nextTransactionCommission;

    /**
     * Minimum daily average balance (VIP accounts).
     */
    private BigDecimal minimumDailyAverage;
}
