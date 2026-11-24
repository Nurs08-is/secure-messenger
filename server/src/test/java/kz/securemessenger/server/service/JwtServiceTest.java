package kz.securemessenger.server.service;

import kz.securemessenger.server.model.User;
import kz.securemessenger.common.model.Role;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Arrays;
import java.util.Date;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    private final byte[] key = new byte[32];

    private final JwtService service =
            new JwtService(
                    Base64.getEncoder()
                            .encodeToString(key));

    private User createUser() {

        User user = mock(User.class);

        when(user.getUsername())
                .thenReturn("employee");

        when(user.getRole())
                .thenReturn(Role.EMPLOYEE);

        return user;
    }

    @Test
    void shouldGenerateToken() {

        String token =
                service.generateToken(createUser());

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldContainUsername() {

        String token =
                service.generateToken(createUser());

        assertEquals(
                "employee",
                service.validateToken(token)
                        .getSubject()
        );
    }

    @Test
    void shouldContainRole() {

        String token =
                service.generateToken(createUser());

        assertEquals(
                "EMPLOYEE",
                service.validateToken(token)
                        .get("role", String.class)
        );
    }

    @Test
    void shouldRejectInvalidToken() {

        assertThrows(JwtException.class, () ->
                service.validateToken("invalid-token"));
    }

    @Test
    void shouldRejectWrongSignature() {

        byte[] anotherKey = new byte[32];

        Arrays.fill(anotherKey, (byte) 1);

        JwtService anotherService =
                new JwtService(
                        Base64.getEncoder()
                                .encodeToString(anotherKey));

        String token =
                service.generateToken(createUser());

        assertThrows(JwtException.class, () ->
                anotherService.validateToken(token));
    }

    @Test
    void shouldRejectExpiredToken() {

        String token = Jwts.builder()
                .subject("employee")
                .expiration(Date.from(
                        Instant.now().minusSeconds(60)))
                .signWith(
                        Keys.hmacShaKeyFor(key),
                        Jwts.SIG.HS256)
                .compact();

        assertThrows(JwtException.class, () ->
                service.validateToken(token));
    }

    @Test
    void shouldRejectShortSecret() {

        String shortKey = Base64.getEncoder()
                .encodeToString(new byte[16]);

        assertThrows(IllegalArgumentException.class, () ->
                new JwtService(shortKey));
    }
}