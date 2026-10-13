
package com.finora.backend;

import com.finora.backend.model.User;
import com.finora.backend.repository.BudgetRepository;
import com.finora.backend.repository.TransactionRepository;
import com.finora.backend.repository.UserRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BudgetController(
            BudgetRepository budgetRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private User currentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @GetMapping
    public List<Map<String, Object>> getBudgets(
            @RequestParam(required = false) String month) {

        User user = currentUser();
        String selectedMonth =
                month == null ? YearMonth.now().toString() : month;

        validateMonth(selectedMonth);

        return budgetRepository
                .findByUser_IdAndBudgetMonth(user.getId(), selectedMonth)
                .stream()
                .map(this::toBudgetResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createBudget(
            @Valid @RequestBody Budget budget) {

        User user = currentUser();
        validateBudget(budget);

        String category = budget.getCategory().trim();
        String month = budget.getBudgetMonth();

        if (budgetRepository.existsByUser_IdAndCategoryIgnoreCaseAndBudgetMonth(
                user.getId(), category, month)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A budget already exists for this category and month");
        }

        budget.setId(null);
        budget.setUser(user);
        budget.setCategory(category);

        return toBudgetResponse(budgetRepository.save(budget));
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody Budget updated) {

        User user = currentUser();
        validateBudget(updated);

        Budget existing = budgetRepository
                .findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found"));

        String category = updated.getCategory().trim();
        String month = updated.getBudgetMonth();

        if (budgetRepository
                .existsByUser_IdAndCategoryIgnoreCaseAndBudgetMonthAndIdNot(
                        user.getId(), category, month, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A budget already exists for this category and month");
        }

        existing.setCategory(category);
        existing.setMonthlyLimit(updated.getMonthlyLimit());
        existing.setBudgetMonth(month);

        return toBudgetResponse(budgetRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(@PathVariable Long id) {
        User user = currentUser();

        Budget existing = budgetRepository
                .findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget not found"));

        budgetRepository.delete(existing);
    }

    private void validateBudget(Budget budget) {
        if (budget.getCategory() == null
                || budget.getCategory().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Category is required");
        }

        if (budget.getMonthlyLimit() == null
                || budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Monthly limit must be greater than zero");
        }

        validateMonth(budget.getBudgetMonth());
    }

    private void validateMonth(String month) {
        try {
            if (month == null || !month.matches("\\d{4}-\\d{2}")) {
                throw new IllegalArgumentException("Invalid month");
            }
            YearMonth.parse(month);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Month must use YYYY-MM format");
        }
    }


private Map<String, Object> toBudgetResponse(Budget budget) {
    YearMonth month = YearMonth.parse(budget.getBudgetMonth());

    BigDecimal spent = transactionRepository.calculateUserCategorySpending(
            budget.getUser().getId(),
            budget.getCategory(),
            month.atDay(1),
            month.plusMonths(1).atDay(1)
    );

    if (spent == null) {
        spent = BigDecimal.ZERO;
    }

    BigDecimal remaining = budget.getMonthlyLimit().subtract(spent);

    BigDecimal percentage = spent
            .multiply(BigDecimal.valueOf(100))
            .divide(budget.getMonthlyLimit(), 2, RoundingMode.HALF_UP);

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", budget.getId());
    response.put("category", budget.getCategory());
    response.put("monthlyLimit", budget.getMonthlyLimit());
    response.put("budgetMonth", budget.getBudgetMonth());
    response.put("spent", spent);
    response.put("remaining", remaining);
    response.put("percentage", percentage);
    response.put("exceeded",
            spent.compareTo(budget.getMonthlyLimit()) > 0);

    return response;
}

}
