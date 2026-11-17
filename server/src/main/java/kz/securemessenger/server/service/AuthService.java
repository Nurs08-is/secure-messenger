package kz.securemessenger.server.service;

import kz.securemessenger.server.model.User;
import kz.securemessenger.server.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import kz.securemessenger.common.model.UserStatus;
import org.springframework.security.authentication.BadCredentialsException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String username, String password,
                         String publicKey) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                "Username is required"
            );
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                "Password must contain at least 8 characters"
            );
        }

        if (publicKey == null || publicKey.isBlank()) {
            throw new IllegalArgumentException(
                "RSA public key is required"
            );
        }

        username = username.trim();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                "Username already exists"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(password);

        User user = new User(
                username, hashedPassword, publicKey
        );

        return userRepository.save(user);
    }

public User authenticate(String username, String password) {

    if (username == null || username.isBlank()
            || password == null) {
        throw new BadCredentialsException(
            "Invalid username or password"
        );
    }

    User user = userRepository
            .findByUsername(username.trim())
            .orElseThrow(() -> new BadCredentialsException(
                    "Invalid username or password"
            ));

    if (!passwordEncoder.matches(
            password, user.getPasswordHash())) {
        throw new BadCredentialsException(
            "Invalid username or password"
        );
    }

    if (user.getStatus() != UserStatus.ACTIVE) {
        throw new BadCredentialsException(
            "Account is not available"
        );
    }

    return user;
}

}