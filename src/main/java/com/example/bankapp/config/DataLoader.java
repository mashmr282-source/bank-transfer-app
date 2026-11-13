package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }

        customerRepository.saveAll(List.of(
                sampleCustomer("100001", "Kamal Silva", "kamal@example.com", "50000.00"),
                sampleCustomer("100002", "Nimali Perera", "nimali@example.com", "25000.00"),
                sampleCustomer("100003", "Saman Fernando", "saman@example.com", "10000.00")
        ));
    }

    private Customer sampleCustomer(String accountNumber, String fullName,
                                    String email, String balance) {
        Customer customer = new Customer();
        customer.setAccountNumber(accountNumber);
        customer.setFullName(fullName);
        customer.setEmail(email);
        customer.setBalance(new BigDecimal(balance));
        return customer;
    }
}