package com.example.banking.customer.dto;

import com.example.banking.customer.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "firstName is required")
        @Size(max = 100, message = "firstName must be at most 100 characters")
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 100, message = "lastName must be at most 100 characters")
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        @Size(max = 254, message = "email must be at most 254 characters")
        String email,

        @NotBlank(message = "phone is required")
        @Size(max = 20, message = "phone must be at most 20 characters")
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "phone must be a valid phone number")
        String phone,

        @NotNull(message = "status is required")
        CustomerStatus status) {
}
