package com.example.bankapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransferRequest {

    @NotBlank(message = "Select a sender account")
    private String fromAccount;

    @NotBlank(message = "Select a receiver account")
    private String toAccount;

    @NotNull(message = "Enter an amount")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Digits(integer = 12, fraction = 2,
            message = "Use up to 12 digits before the decimal point and 2 after it")
    private BigDecimal amount;

    private boolean simulateFailure;
}