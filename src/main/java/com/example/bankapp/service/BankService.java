package com.example.bankapp.service;

import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    public List<Customer> getCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAllOrdered();
        }
        return customerRepository.searchByName(keyword.trim());
    }

    public Customer getCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public long getTotalCustomers() {
        return customerRepository.countCustomersNative();
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total == null ? BigDecimal.ZERO : total;
    }

    public List<BankTransaction> getLatestTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public long countTransactions(TransactionStatus status) {
        return transactionRepository.countByStatus(status);
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customerRepository.countAccounts(customer.getAccountNumber()) > 0) {
            throw new BankException("This account number already exists");
        }

        if (customerRepository.countEmails(customer.getEmail()) > 0) {
            throw new BankException("This email address already exists");
        }

        return customerRepository.save(customer);
    }

@Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(String fromAccount, String toAccount,
                                    BigDecimal amount, boolean simulateFailure) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new BankException("Enter a positive amount with up to 2 decimal places");
        }

        if (fromAccount == null || fromAccount.isBlank()
                || toAccount == null || toAccount.isBlank()) {
            throw new BankException("Select a sender and a receiver");
        }

        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver must be different accounts");
        }

        getCustomer(fromAccount);
        getCustomer(toAccount);

        int debitedRows = customerRepository.debit(fromAccount, amount);
        if (debitedRows == 0) {
            throw new InsufficientBalanceException(fromAccount);
        }

        if (simulateFailure) {
            throw new BankException("Simulated system failure. All changes were rolled back.");
        }

        int creditedRows = customerRepository.credit(toAccount, amount);
        if (creditedRows != 1) {
            throw new AccountNotFoundException(toAccount);
        }

        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Transfer completed successfully");

        return transactionRepository.save(transaction);
    }
}