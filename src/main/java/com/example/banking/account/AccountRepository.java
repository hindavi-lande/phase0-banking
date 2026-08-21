package com.example.banking.account;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByAccountNumberAndIdNot(String accountNumber, UUID id);

    boolean existsByCustomerId(UUID customerId);
}
