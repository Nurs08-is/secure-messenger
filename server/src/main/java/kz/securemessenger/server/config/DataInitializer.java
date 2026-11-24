package kz.securemessenger.server.config;

import kz.securemessenger.server.model.User;
import kz.securemessenger.server.repository.UserRepository;
import kz.securemessenger.common.model.Role;
import kz.securemessenger.common.model.UserStatus;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            createUser(
                    userRepository,
                    passwordEncoder,
                    "admin",
                    System.getenv("ADMIN_PASSWORD"),
                    Role.ADMIN
            );

            createUser(
                    userRepository,
                    passwordEncoder,
                    "hr",
                    System.getenv("HR_PASSWORD"),
                    Role.HR
            );
        };
    }

    private void createUser(
            UserRepository repository,
            PasswordEncoder encoder,
            String username,
            String password,
            Role role) {

        if (repository.existsByUsername(username)) {
            return;
        }

        if (password == null || password.length() < 12) {
            System.out.println(
                    "Initial account skipped: " + username
            );
            return;
        }

        User user = new User(
                username,
                encoder.encode(password),
                null
        );

        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        repository.save(user);

        System.out.println(
                "Initial account created: " + username
        );
    }
}