package com.example.banking.account;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.banking.customer.Customer;
import com.example.banking.customer.CustomerRepository;
import com.example.banking.customer.CustomerStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private UUID customerId;

    @BeforeEach
    void seedCustomer() {
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer customer = customerRepository.save(
                new Customer("Ada", "Lovelace", "ada@example.com", "+44 20 7946 0958", CustomerStatus.ACTIVE));
        customerId = customer.getId();
    }

    private static String accountJson(UUID customerId, String number, String type, String balance, String status) {
        return """
                {
                  "customerId": "%s",
                  "accountNumber": "%s",
                  "type": "%s",
                  "balance": %s,
                  "status": "%s"
                }
                """.formatted(customerId, number, type, balance, status);
    }

    @Test
    void fullCrudLifecycleAcrossTheForeignKey() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, "ACC-0001", "SAVINGS", "250.00", "ACTIVE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.accountNumber").value("ACC-0001"))
                .andExpect(jsonPath("$.type").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(250.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String id = objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asText();

        mockMvc.perform(get("/api/accounts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.accountNumber").value("ACC-0001"));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(hasSize(1)));

        mockMvc.perform(put("/api/accounts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, "ACC-0002", "CURRENT", "10.50", "CLOSED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC-0002"))
                .andExpect(jsonPath("$.type").value("CURRENT"))
                .andExpect(jsonPath("$.balance").value(10.50))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(delete("/api/accounts/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void createWithUnknownCustomerReturns404() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(
                                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                "ACC-9999",
                                "SAVINGS",
                                "0.00",
                                "ACTIVE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Customer not found")));
    }

    @Test
    void negativeBalanceReturns400() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, "ACC-0003", "SAVINGS", "-1.00", "ACTIVE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.balance").exists());
    }

    @Test
    void unknownEnumValueReturns400() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, "ACC-0004", "FIXED_DEPOSIT", "0.00", "ACTIVE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateAccountNumberReturns409() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, "ACC-DUP", "SAVINGS", "0.00", "ACTIVE")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, "ACC-DUP", "CURRENT", "0.00", "ACTIVE")))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingCustomerWithAccountsReturns409() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountJson(customerId, "ACC-LINKED", "SAVINGS", "0.00", "ACTIVE")))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/customers/{id}", customerId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
