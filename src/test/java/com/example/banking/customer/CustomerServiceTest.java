package com.example.banking.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.banking.account.AccountRepository;
import com.example.banking.common.DuplicateResourceException;
import com.example.banking.common.ResourceInUseException;
import com.example.banking.common.ResourceNotFoundException;
import com.example.banking.customer.dto.CustomerRequest;
import com.example.banking.customer.dto.CustomerResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRequest request;

    @BeforeEach
    void setUp() {
        request = new CustomerRequest("Ada", "Lovelace", "ada@example.com", "+44 20 7946 0958", CustomerStatus.ACTIVE);
    }

    @Test
    void createPersistsCustomer() {
        when(customerRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.create(request);

        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(customerRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ada@example.com");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void listMapsEveryCustomer() {
        when(customerRepository.findAll()).thenReturn(List.of(
                new Customer("Ada", "Lovelace", "ada@example.com", "111", CustomerStatus.ACTIVE),
                new Customer("Alan", "Turing", "alan@example.com", "222", CustomerStatus.INACTIVE)));

        List<CustomerResponse> customers = customerService.list();

        assertThat(customers).hasSize(2);
        assertThat(customers).extracting(CustomerResponse::lastName).containsExactly("Lovelace", "Turing");
    }

    @Test
    void updateAppliesEveryField() {
        UUID id = UUID.randomUUID();
        Customer existing = new Customer("Ada", "Lovelace", "ada@example.com", "111", CustomerStatus.ACTIVE);
        when(customerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByEmailIgnoreCaseAndIdNot("grace@example.com", id)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.update(
                id,
                new CustomerRequest("Grace", "Hopper", "grace@example.com", "999", CustomerStatus.INACTIVE));

        assertThat(response.firstName()).isEqualTo("Grace");
        assertThat(response.lastName()).isEqualTo("Hopper");
        assertThat(response.email()).isEqualTo("grace@example.com");
        assertThat(response.phone()).isEqualTo("999");
        assertThat(response.status()).isEqualTo(CustomerStatus.INACTIVE);
    }

    @Test
    void deleteRemovesCustomerWithoutAccounts() {
        UUID id = UUID.randomUUID();
        Customer existing = new Customer("Ada", "Lovelace", "ada@example.com", "111", CustomerStatus.ACTIVE);
        when(customerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByCustomerId(id)).thenReturn(false);

        customerService.delete(id);

        verify(customerRepository).delete(existing);
    }

    @Test
    void deleteRejectsCustomerWithAccounts() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id))
                .thenReturn(Optional.of(
                        new Customer("Ada", "Lovelace", "ada@example.com", "111", CustomerStatus.ACTIVE)));
        when(accountRepository.existsByCustomerId(id)).thenReturn(true);

        assertThatThrownBy(() -> customerService.delete(id))
                .isInstanceOf(ResourceInUseException.class);

        verify(customerRepository, never()).delete(any());
    }
}
