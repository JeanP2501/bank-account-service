package com.bank.account.model.dto;

import com.bank.account.model.enums.CustomerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for customer information from Customer Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    private String id;
    private CustomerType customerType;
    private String documentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String businessName;
    private String taxId;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean hasCreditCard;
}
