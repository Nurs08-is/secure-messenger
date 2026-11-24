package kz.securemessenger.client;

import java.security.KeyPair;

public class SessionManager {

    private String token;
    private String username;
    private String role;
    private KeyPair keyPair;

    public void login(
            String token,
            String username,
            String role,
            KeyPair keyPair) {

        this.token = token;
        this.username = username;
        this.role = role;
        this.keyPair = keyPair;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public boolean isLoggedIn() {
        return token != null && !token.isBlank();
    }

    public boolean hasRole(String requiredRole) {
        return requiredRole != null
                && requiredRole.equals(role);
    }

    public void logout() {
        token = null;
        username = null;
        role = null;
        keyPair = null;
    }
}