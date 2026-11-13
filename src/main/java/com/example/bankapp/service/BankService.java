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
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private static final String FEE_ACCOUNT_NUMBER = "999999";
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");

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

    public List<BankTransaction> getLargeTransfers(BigDecimal min) {
        return transactionRepository.findTransfersAbove(min);
    }

    public BigDecimal getTotalSent(String accountNumber) {
        BigDecimal total = transactionRepository.sumSentByAccount(
                accountNumber, TransactionStatus.SUCCESS);
        return total == null ? BigDecimal.ZERO : total;
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
        validateAmount(amount);

        if (fromAccount == null || fromAccount.isBlank()
                || toAccount == null || toAccount.isBlank()) {
            throw new BankException("Select a sender and a receiver");
        }

        if (fromAccount.equals(toAccount)) {
            throw new BankException("Sender and receiver must be different accounts");
        }

        if (FEE_ACCOUNT_NUMBER.equals(fromAccount) || FEE_ACCOUNT_NUMBER.equals(toAccount)) {
            throw new BankException("The bank fee account is reserved for collecting fees");
        }

        getCustomer(fromAccount);
        getCustomer(toAccount);

        BigDecimal fee = amount.movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);

        if (customerRepository.debit(fromAccount, totalDebit) != 1) {
            throw new InsufficientBalanceException(fromAccount);
        }

        if (simulateFailure) {
            throw new BankException("Simulated system failure. All changes were rolled back.");
        }

        if (customerRepository.credit(toAccount, amount) != 1) {
            throw new AccountNotFoundException(toAccount);
        }

        if (fee.signum() > 0
                && customerRepository.credit(FEE_ACCOUNT_NUMBER, fee) != 1) {
            throw new AccountNotFoundException(FEE_ACCOUNT_NUMBER);
        }

        return saveSuccessfulTransaction(
                fromAccount, toAccount, amount,
                "Transfer completed successfully. Fee: LKR " + fee.toPlainString());
    }

    @Transactional
    public void deposit(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        getCustomer(accountNumber);

        if (customerRepository.credit(accountNumber, amount) != 1) {
            throw new AccountNotFoundException(accountNumber);
        }

        saveSuccessfulTransaction(null, accountNumber, amount, "Deposit");
    }

    @Transactional
    public void withdraw(String accountNumber, BigDecimal amount) {
        validateAmount(amount);
        getCustomer(accountNumber);

        if (customerRepository.debit(accountNumber, amount) != 1) {
            throw new InsufficientBalanceException(accountNumber);
        }

        saveSuccessfulTransaction(accountNumber, null, amount, "Withdrawal");
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0
                || amount.scale() > 2 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BankException(
                    "Enter a positive amount with up to 12 digits before the decimal point and 2 after it");
        }
    }

    private BankTransaction saveSuccessfulTransaction(String fromAccount, String toAccount,
                                                      BigDecimal amount, String description) {
        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }
}