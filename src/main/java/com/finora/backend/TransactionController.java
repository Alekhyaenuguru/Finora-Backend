
package com.finora.backend;

import com.finora.backend.model.User;
import com.finora.backend.repository.TransactionRepository;
import com.finora.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionRepository repository;
    private final UserRepository userRepository;

    public TransactionController(
            TransactionRepository repository,
            UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private User currentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
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
    public List<Transaction> getAll() {
        User user = currentUser();
        return repository.findAllByUser_Id(user.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable Long id) {
        User user = currentUser();

        return repository.findByIdAndUser_Id(id, user.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Transaction create(@RequestBody Transaction transaction) {
        User user = currentUser();

        transaction.setId(null);
        transaction.setUser(user);

        return repository.save(transaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(
            @PathVariable Long id,
            @RequestBody Transaction updated) {

        User user = currentUser();

        return repository.findByIdAndUser_Id(id, user.getId())
                .map(existing -> {
                    existing.setTitle(updated.getTitle());
                    existing.setType(updated.getType());
                    existing.setAmount(updated.getAmount());
                    existing.setCategory(updated.getCategory());
                    existing.setDate(updated.getDate());
                    existing.setNote(updated.getNote());

                    // Keep the original owner; never trust the request body.
                    return ResponseEntity.ok(repository.save(existing));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User user = currentUser();

        return repository.findByIdAndUser_Id(id, user.getId())
                .map(transaction -> {
                    repository.delete(transaction);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
