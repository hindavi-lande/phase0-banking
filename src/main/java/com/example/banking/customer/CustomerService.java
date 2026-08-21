package com.example.banking.customer;

import com.example.banking.account.AccountRepository;
import com.example.banking.common.DuplicateResourceException;
import com.example.banking.common.ResourceInUseException;
import com.example.banking.common.ResourceNotFoundException;
import com.example.banking.customer.dto.CustomerRequest;
import com.example.banking.customer.dto.CustomerResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public CustomerService(CustomerRepository customerRepository, AccountRepository accountRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Customer already exists with email: " + request.email());
        }

        Customer customer = new Customer(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.status());

        return CustomerResponse.from(customerRepository.save(customer));
    }

    public CustomerResponse get(UUID id) {
        return CustomerResponse.from(findOrThrow(id));
    }

    public List<CustomerResponse> list() {
        return customerRepository.findAll().stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = findOrThrow(id);

        if (customerRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("Customer already exists with email: " + request.email());
        }

        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setStatus(request.status());

        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional
    public void delete(UUID id) {
        Customer customer = findOrThrow(id);

        // Reject rather than let the accounts.customer_id constraint surface as a 500.
        if (accountRepository.existsByCustomerId(id)) {
            throw new ResourceInUseException("Customer cannot be deleted while accounts still reference it: " + id);
        }

        customerRepository.delete(customer);
    }

    /** Shared lookup so the Account slice can resolve the FK without duplicating the 404. */
    public Customer findOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }
}
