package com.example.banking.account.dto;

import com.example.banking.account.Account;
import com.example.banking.account.AccountStatus;
import com.example.banking.account.AccountType;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String accountNumber,
        AccountType type,
        BigDecimal balance,
        AccountStatus status) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomer().getId(),
                account.getAccountNumber(),
                account.getType(),
                account.getBalance(),
                account.getStatus());
    }
}
