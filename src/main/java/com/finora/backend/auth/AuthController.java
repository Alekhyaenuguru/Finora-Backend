
package com.finora.backend.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record RegisterRequest(
            @NotBlank
            @Size(max = 100)
            String name,

            @NotBlank
            @Email
            @Size(max = 190)
            String email,

            @NotBlank
            @Size(min = 8, max = 72)
            String password) {
    }

    public record LoginRequest(
            @NotBlank
            @Email
            String email,

            @NotBlank
            String password) {
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(authService.register(
                            request.name(),
                            request.email(),
                            request.password()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(
                    authService.login(
                            request.email(),
                            request.password()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", ex.getMessage()));
        }
    }
}
