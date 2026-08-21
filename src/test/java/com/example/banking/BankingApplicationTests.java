package com.example.banking;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.banking.account.AccountController;
import com.example.banking.customer.CustomerController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BankingApplicationTests {

    @Autowired
    private CustomerController customerController;

    @Autowired
    private AccountController accountController;

    @Test
    void contextLoadsWithBothSlices() {
        assertThat(customerController).isNotNull();
        assertThat(accountController).isNotNull();
    }
}
