
package com.finora.backend.repository;

import com.finora.backend.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUser_IdAndBudgetMonth(
            Long userId,
            String budgetMonth
    );

    Optional<Budget> findByIdAndUser_Id(
            Long id,
            Long userId
    );

    boolean existsByUser_IdAndCategoryIgnoreCaseAndBudgetMonth(
            Long userId,
            String category,
            String budgetMonth
    );

    boolean existsByUser_IdAndCategoryIgnoreCaseAndBudgetMonthAndIdNot(
            Long userId,
            String category,
            String budgetMonth,
            Long id
    );

    boolean existsByIdAndUser_Id(Long id, Long userId);
}
