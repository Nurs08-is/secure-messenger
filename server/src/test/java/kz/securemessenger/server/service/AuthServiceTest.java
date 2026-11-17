package kz.securemessenger.server.service;

import kz.securemessenger.server.model.User;
import kz.securemessenger.server.repository.UserRepository;
import kz.securemessenger.common.model.UserStatus;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserRepository repository =
            mock(UserRepository.class);

    private final PasswordEncoder encoder =
            new BCryptPasswordEncoder();

    private final AuthService service =
            new AuthService(repository, encoder);

    @Test
    void registerShouldHashPassword() {

        when(repository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        User user = service.register(
                "nursultan",
                "password123",
                "test-public-key"
        );

        assertNotEquals("password123", user.getPasswordHash());
        assertTrue(encoder.matches(
                "password123", user.getPasswordHash()
        ));
    }

    @Test
    void newUserShouldBePending() {

        when(repository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        User user = service.register(
                "employee",
                "password123",
                "test-public-key"
        );

        assertEquals(UserStatus.PENDING, user.getStatus());
    }

    @Test
    void duplicateUsernameShouldFail() {

        when(repository.existsByUsername("nursultan"))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                service.register(
                        "nursultan",
                        "password123",
                        "test-public-key"
                )
        );
    }
}
