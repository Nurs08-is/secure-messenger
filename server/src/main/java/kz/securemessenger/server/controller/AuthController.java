package kz.securemessenger.server.controller;

import kz.securemessenger.server.model.User;
import kz.securemessenger.server.service.AuthService;
import kz.securemessenger.server.service.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import kz.securemessenger.server.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final AuditService auditService;



    public AuthController(AuthService authService,
                      JwtService jwtService,
                      AuditService auditService) {

    this.authService = authService;
    this.jwtService = jwtService;
    this.auditService = auditService;
}
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        try {
            User user = authService.register(
                    request.username(),
                    request.password(),
                    request.publicKey()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "username", user.getUsername(),
                            "status", user.getStatus().name(),
                            "role", user.getRole().name()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        try {
            User user = authService.authenticate(
                    request.username(),
                    request.password()
            );

            String token = jwtService.generateToken(user);
            auditService.log(
        user.getUsername(),
        "LOGIN_SUCCESS",
        "Authentication successful"
);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", user.getUsername(),
                    "role", user.getRole().name()
            ));

        } catch (BadCredentialsException e) {
            auditService.logFailedLogin(request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login failed"));
        }
    }

    public record RegisterRequest(
            String username,
            String password,
            String publicKey
    ) {}

    public record LoginRequest(
            String username,
            String password
    ) {}
}