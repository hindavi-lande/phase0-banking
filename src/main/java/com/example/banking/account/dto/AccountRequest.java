package com.example.banking.account.dto;

import com.example.banking.account.AccountStatus;
import com.example.banking.account.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountRequest(
        @NotNull(message = "customerId is required")
        UUID customerId,

        @NotBlank(message = "accountNumber is required")
        @Size(max = 34, message = "accountNumber must be at most 34 characters")
        String accountNumber,

        @NotNull(message = "type is required")
        AccountType type,

        @NotNull(message = "balance is required")
        @DecimalMin(value = "0.00", message = "balance must not be negative")
        @Digits(integer = 17, fraction = 2, message = "balance must have at most 2 decimal places")
        BigDecimal balance,

        @NotNull(message = "status is required")
        AccountStatus status) {
}
