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

    /**
     * Minimum opening amount required for this account.
     * Can be zero for standard accounts
     */
    private BigDecimal minimumOpeningAmount = BigDecimal.ZERO;

    /**
     * Number of free transactions per month (deposits + withdrawals).
     * Default: 5 transactions
     */
    private Integer freeTransactionsPerMonth = 5;

    /**
     * Commission charged per transaction after free limit.
     * Default: 2.00
     */
    private BigDecimal commissionPerTransaction = new BigDecimal("2.00");

    /**
     * Transaction count for current month.
     * Resets every month
     */
    private Integer currentMonthTransactionCount = 0;

    /**
     * Month of last transaction (1-12).
     * Used to reset counter each month
     */
    private Integer lastTransactionMonth;

    /**
     * Year of last transaction.
     * Used with month to reset counter
     */
    private Integer lastTransactionYear;

    /**
     * Minimum daily average balance required (for VIP accounts).
     * Null for standard accounts
     */
    private BigDecimal minimumDailyAverage;

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

    /**
     * Check if account has free transactions available.
     * @return true if under free transaction limit
     */
    public boolean hasFreeTransactionsAvailable() {
        return currentMonthTransactionCount < freeTransactionsPerMonth;
    }

    /**
     * Get commission for next transaction.
     * @return commission amount (0 if free transactions available)
     */
    public BigDecimal getNextTransactionCommission() {
        return hasFreeTransactionsAvailable()
                ? BigDecimal.ZERO
                : commissionPerTransaction;
    }

    /**
     * Increment transaction counter.
     * Resets counter if in new month
     * @param currentMonth current month (1-12)
     * @param currentYear current year
     */
    public void incrementTransactionCount(int currentMonth, int currentYear) {
        // Reset counter if new month
        if (lastTransactionMonth == null ||
                lastTransactionMonth != currentMonth ||
                lastTransactionYear != currentYear) {
            this.currentMonthTransactionCount = 0;
            this.lastTransactionMonth = currentMonth;
            this.lastTransactionYear = currentYear;
        }

        this.currentMonthTransactionCount++;
    }

    /**
     * Check if account is VIP type.
     * @return true if PERSONAL_VIP
     */
    public boolean isVipAccount() {
        // Esta lógica depende de cómo guardes el tipo de cuenta
        // Podrías agregar un campo accountSubtype
        return false; // Implementar según diseño
    }
}
