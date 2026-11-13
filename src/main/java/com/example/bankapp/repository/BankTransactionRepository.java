package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    @Query("""
        SELECT t FROM BankTransaction t
        ORDER BY t.createdAt DESC, t.id DESC
        """)
    List<BankTransaction> findAllLatest();

    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.fromAccount = :account OR t.toAccount = :account
        ORDER BY t.createdAt DESC, t.id DESC
        """)
    List<BankTransaction> findHistory(@Param("account") String account);

    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.status = :status
        ORDER BY t.createdAt DESC, t.id DESC
        """)
    List<BankTransaction> findByStatusLatest(
            @Param("status") TransactionStatus status);

    @Query("""
        SELECT COUNT(t) FROM BankTransaction t
        WHERE t.status = :status
        """)
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("""
        SELECT t FROM BankTransaction t
        WHERE t.amount > :min
          AND t.fromAccount IS NOT NULL
          AND t.toAccount IS NOT NULL
        ORDER BY t.createdAt DESC, t.id DESC
        """)
    List<BankTransaction> findTransfersAbove(@Param("min") BigDecimal min);

    @Query("""
        SELECT SUM(t.amount) FROM BankTransaction t
        WHERE t.fromAccount = :account
          AND t.status = :status
          AND t.toAccount IS NOT NULL
        """)
    BigDecimal sumSentByAccount(
            @Param("account") String account,
            @Param("status") TransactionStatus status);
}