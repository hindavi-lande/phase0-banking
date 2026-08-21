package com.example.banking.account;

import com.example.banking.account.dto.AccountRequest;
import com.example.banking.account.dto.AccountResponse;
import com.example.banking.common.DuplicateResourceException;
import com.example.banking.common.ResourceNotFoundException;
import com.example.banking.customer.Customer;
import com.example.banking.customer.CustomerService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerService customerService;

    public AccountService(AccountRepository accountRepository, CustomerService customerService) {
        this.accountRepository = accountRepository;
        this.customerService = customerService;
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.accountNumber())) {
            throw new DuplicateResourceException(
                    "Account already exists with accountNumber: " + request.accountNumber());
        }

        Customer customer = customerService.findOrThrow(request.customerId());

        Account account = new Account(
                customer,
                request.accountNumber(),
                request.type(),
                request.balance(),
                request.status());

        return AccountResponse.from(accountRepository.save(account));
    }

    public AccountResponse get(UUID id) {
        return AccountResponse.from(findOrThrow(id));
    }

    public List<AccountResponse> list() {
        return accountRepository.findAll().stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional
    public AccountResponse update(UUID id, AccountRequest request) {
        Account account = findOrThrow(id);

        if (accountRepository.existsByAccountNumberAndIdNot(request.accountNumber(), id)) {
            throw new DuplicateResourceException(
                    "Account already exists with accountNumber: " + request.accountNumber());
        }

        // Re-resolve the FK so an account can be moved to a different customer.
        account.setCustomer(customerService.findOrThrow(request.customerId()));
        account.setAccountNumber(request.accountNumber());
        account.setType(request.type());
        account.setBalance(request.balance());
        account.setStatus(request.status());

        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public void delete(UUID id) {
        accountRepository.delete(findOrThrow(id));
    }

    private Account findOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }
}
