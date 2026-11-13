package com.example.bankapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Enter the customer's full name")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Enter an email address")
    @Email(message = "Enter a valid email address")
    @Size(max = 254)
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @NotBlank(message = "Enter an account number")
    @Pattern(regexp = "[0-9]{6}", message = "Enter a 6-digit account number")
    @Column(name = "account_number", nullable = false, unique = true, length = 6)
    private String accountNumber;

    @NotNull(message = "Enter an opening balance")
    @DecimalMin(value = "0.00", message = "Opening balance cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Use up to 12 digits and 2 decimal places")
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance;
}