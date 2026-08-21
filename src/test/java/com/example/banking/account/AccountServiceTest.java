package com.example.banking.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.banking.account.dto.AccountRequest;
import com.example.banking.account.dto.AccountResponse;
import com.example.banking.common.DuplicateResourceException;
import com.example.banking.common.ResourceNotFoundException;
import com.example.banking.customer.Customer;
import com.example.banking.customer.CustomerService;
import com.example.banking.customer.CustomerStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private AccountService accountService;

    private UUID customerId;
    private Customer customer;
    private AccountRequest request;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customer = new Customer("Ada", "Lovelace", "ada@example.com", "111", CustomerStatus.ACTIVE);
        request = new AccountRequest(
                customerId, "ACC-0001", AccountType.SAVINGS, new BigDecimal("250.00"), AccountStatus.ACTIVE);
    }

    @Test
    void createResolvesForeignKeyAndPersists() {
        when(accountRepository.existsByAccountNumber("ACC-0001")).thenReturn(false);
        when(customerService.findOrThrow(customerId)).thenReturn(customer);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.create(request);

        assertThat(response.accountNumber()).isEqualTo("ACC-0001");
        assertThat(response.type()).isEqualTo(AccountType.SAVINGS);
        assertThat(response.balance()).isEqualByComparingTo("250.00");
        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        verify(customerService).findOrThrow(customerId);
    }

    @Test
    void createRejectsDuplicateAccountNumber() {
        when(accountRepository.existsByAccountNumber("ACC-0001")).thenReturn(true);

        assertThatThrownBy(() -> accountService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ACC-0001");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void createFailsWhenCustomerMissing() {
        when(accountRepository.existsByAccountNumber("ACC-0001")).thenReturn(false);
        when(customerService.findOrThrow(customerId))
                .thenThrow(new ResourceNotFoundException("Customer", customerId));

        assertThatThrownBy(() -> accountService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account");
    }

    @Test
    void updateReassignsCustomerAndFields() {
        UUID id = UUID.randomUUID();
        UUID newCustomerId = UUID.randomUUID();
        Customer newCustomer = new Customer("Alan", "Turing", "alan@example.com", "222", CustomerStatus.ACTIVE);

        Account existing =
                new Account(customer, "ACC-0001", AccountType.SAVINGS, new BigDecimal("250.00"), AccountStatus.ACTIVE);

        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByAccountNumberAndIdNot("ACC-0002", id)).thenReturn(false);
        when(customerService.findOrThrow(newCustomerId)).thenReturn(newCustomer);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.update(
                id,
                new AccountRequest(
                        newCustomerId, "ACC-0002", AccountType.CURRENT, new BigDecimal("10.50"), AccountStatus.CLOSED));

        assertThat(response.accountNumber()).isEqualTo("ACC-0002");
        assertThat(response.type()).isEqualTo(AccountType.CURRENT);
        assertThat(response.balance()).isEqualByComparingTo("10.50");
        assertThat(response.status()).isEqualTo(AccountStatus.CLOSED);
        assertThat(existing.getCustomer()).isSameAs(newCustomer);
    }

    @Test
    void deleteRemovesAccount() {
        UUID id = UUID.randomUUID();
        Account existing =
                new Account(customer, "ACC-0001", AccountType.SAVINGS, new BigDecimal("250.00"), AccountStatus.ACTIVE);
        when(accountRepository.findById(id)).thenReturn(Optional.of(existing));

        accountService.delete(id);

        verify(accountRepository).delete(existing);
    }
}
