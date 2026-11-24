package kz.securemessenger.client;

import kz.securemessenger.crypto.KeyStorageService;
import kz.securemessenger.crypto.KeyUtils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LoginController {

    private final ApiClient api;
    private final SessionManager session;

    private final KeyStorageService storage =
            new KeyStorageService();

    public LoginController(
            ApiClient api,
            SessionManager session) {

        this.api = api;
        this.session = session;
    }

    public Parent createView(
            Runnable onLogin,
            Runnable onRegister) {

        Label title = new Label("Secure Messenger");
        title.getStyleClass().add("title");

        Label subtitle = new Label(
                "Қауіпсіз корпоративтік мессенджер");

        TextField username = new TextField();
        username.setPromptText("Логин");

        PasswordField password = new PasswordField();
        password.setPromptText("Құпиясөз");

        Label status = new Label();
        status.setWrapText(true);

        Button login = new Button("Кіру");
        Button register = new Button("Аккаунт ашу");

        login.setMaxWidth(Double.MAX_VALUE);
        register.setMaxWidth(Double.MAX_VALUE);

        login.setOnAction(event -> {

            String name = username.getText().trim();
            String pass = password.getText();

            if (name.isBlank() || pass.isBlank()) {
                status.setText(
                        "Логин мен құпиясөзді енгізіңіз");
                return;
            }

            login.setDisable(true);
            register.setDisable(true);

            status.setText("Тексерілуде...");

            CompletableFuture.runAsync(() -> {

                char[] secret = pass.toCharArray();

                try {

                    var response = api.post(
                            "/api/auth/login",
                            Map.of(
                                    "username", name,
                                    "password", pass
                            ));

                    String token =
                            response.get("token").asText();

                    String user =
                            response.get("username").asText();

                    String role =
                            response.get("role").asText();

                    session.login(
                            token, user, role, null);

                    var profile = api.get("/api/users/me");

                    String publicKey = profile
                            .path("publicKey").asText("");

                    KeyPair pair = null;

                    if (!publicKey.isBlank()) {

                        Path path =
                                RegisterController.keyPath(user);

                        if (!Files.exists(path)) {
                            throw new IllegalStateException(
                                    "RSA key file not found");
                        }

                        pair = storage.load(path, secret);

                        if (!publicKey.equals(
                                KeyUtils.encodePublicKey(
                                        pair.getPublic()))) {

                            throw new IllegalStateException(
                                    "RSA key mismatch");
                        }
                    }

                    session.login(token, user, role, pair);

                    Platform.runLater(() -> {
                        password.clear();
                        onLogin.run();
                    });

                } catch (Exception e) {

                    session.logout();

                    Platform.runLater(() -> {

                        status.setText(
                                "Кіру қатесі: " + e.getMessage());

                        login.setDisable(false);
                        register.setDisable(false);
                    });

                } finally {
                    Arrays.fill(secret, '\0');
                }
            });
        });

        password.setOnAction(event -> login.fire());

        register.setOnAction(event -> onRegister.run());

        VBox root = new VBox(
                18,
                title,
                subtitle,
                username,
                password,
                login,
                register,
                status
        );

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("auth-page");

        return root;
    }
}