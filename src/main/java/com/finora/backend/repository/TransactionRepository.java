
package com.finora.backend.repository;

import com.finora.backend.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByUser_Id(Long userId);

    Optional<Transaction> findByIdAndUser_Id(Long id, Long userId);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND UPPER(t.type) = 'EXPENSE'
          AND LOWER(t.category) = LOWER(:category)
          AND t.date >= :startDate
          AND t.date < :endDate
        """)
    BigDecimal calculateUserCategorySpending(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
