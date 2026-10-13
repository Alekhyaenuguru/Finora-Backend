
package com.finora.backend;

import com.finora.backend.repository.TransactionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TransactionRepository repository;

    public DashboardController(TransactionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {

        var transactions = repository.findAll();

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount();

            if (amount == null) {
                continue;
            }

            if ("INCOME".equalsIgnoreCase(transaction.getType())) {
                totalIncome = totalIncome.add(amount);
            } else if ("EXPENSE".equalsIgnoreCase(transaction.getType())) {
                totalExpenses = totalExpenses.add(amount);
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpenses", totalExpenses);
        summary.put("balance", totalIncome.subtract(totalExpenses));
        summary.put("transactionCount", transactions.size());

        return summary;
    }
}
