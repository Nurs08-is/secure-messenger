package kz.securemessenger.client;

import kz.securemessenger.crypto.KeyStorageService;
import kz.securemessenger.crypto.KeyUtils;
import kz.securemessenger.crypto.RsaKeyService;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RegisterController {

    private final ApiClient api;

    private final RsaKeyService rsa =
            new RsaKeyService();

    private final KeyStorageService storage =
            new KeyStorageService();

    public RegisterController(ApiClient api) {
        this.api = api;
    }

    public Parent createView(Runnable onBack) {

        Label title = new Label("Аккаунт ашу");
        title.getStyleClass().add("title");

        TextField username = new TextField();
        username.setPromptText("Логин");

        PasswordField password = new PasswordField();
        password.setPromptText("Құпиясөз");

        PasswordField confirm = new PasswordField();
        confirm.setPromptText("Құпиясөзді қайталаңыз");

        Label status = new Label();
        status.setWrapText(true);

        Button register = new Button("Тіркелу");
        Button back = new Button("Кіруге қайту");

        register.setMaxWidth(Double.MAX_VALUE);
        back.setMaxWidth(Double.MAX_VALUE);

        register.setOnAction(event -> {

            String login = username.getText().trim();
            String pass = password.getText();

            if (login.isBlank() || login.length() > 100) {
                status.setText("Логинді дұрыс енгізіңіз");
                return;
            }

            if (pass.length() < 8 ||
                    pass.getBytes(StandardCharsets.UTF_8).length > 72) {

                status.setText("Құпиясөз ұзындығы жарамсыз");
                return;
            }

            if (!pass.equals(confirm.getText())) {
                status.setText("Құпиясөздер сәйкес емес");
                return;
            }

            register.setDisable(true);
            back.setDisable(true);

            status.setText("Тіркелу орындалуда...");

            CompletableFuture.runAsync(() -> {

                char[] secret = pass.toCharArray();

                try {

                    Path path = keyPath(login);

                    KeyPair pair;

                    if (Files.exists(path)) {
                        pair = storage.load(path, secret);
                    } else {
                        pair = rsa.generateKeyPair();
                        storage.save(path, pair, secret);
                    }

                    api.post("/api/auth/register", Map.of(
                            "username", login,
                            "password", pass,
                            "publicKey", KeyUtils.encodePublicKey(
                                    pair.getPublic())
                    ));

                    Platform.runLater(() -> {
                        status.setText(
                                "Тіркелу сәтті өтті. HR растауын күтіңіз.");

                        password.clear();
                        confirm.clear();
                        back.setDisable(false);
                    });

                } catch (Exception e) {

                    Platform.runLater(() -> {
                        status.setText(
                                "Тіркелу қатесі: " + e.getMessage());

                        register.setDisable(false);
                        back.setDisable(false);
                    });

                } finally {
                    Arrays.fill(secret, '\0');
                }
            });
        });

        back.setOnAction(event -> onBack.run());

        VBox root = new VBox(
                18,
                title,
                username,
                password,
                confirm,
                register,
                back,
                status
        );

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setFillWidth(true);
        root.getStyleClass().add("auth-page");

        return root;
    }

    public static Path keyPath(String username)
            throws Exception {

        byte[] hash = MessageDigest
                .getInstance("SHA-256")
                .digest(username.trim()
                        .getBytes(StandardCharsets.UTF_8));

        String filename =
                HexFormat.of().formatHex(hash) + ".key";

        return Path.of(
                System.getProperty("user.home"),
                ".secure-messenger",
                "keystore",
                filename
        );
    }
}