package com.example.banking.customer;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Query("SELECT c FROM Customer c WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) "
            + "ORDER BY c.id")
    List<Customer> searchByNameOrEmail(@Param("query") String query);
}
