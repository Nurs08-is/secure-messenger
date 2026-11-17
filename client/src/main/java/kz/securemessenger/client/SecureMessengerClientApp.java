
package kz.securemessenger.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SecureMessengerClientApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Secure Messenger");
        showLogin(stage);
        stage.show();
    }

    // Логин терезесі
    private void showLogin(Stage stage) {
        Label title = new Label("Secure Messenger");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField username = new TextField();
        username.setPromptText("Логин");

        PasswordField password = new PasswordField();
        password.setPromptText("Құпиясөз");

        Label error = new Label();
        error.setStyle("-fx-text-fill: red;");

        Button loginButton = new Button("Кіру");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        loginButton.setOnAction(event -> {
            if (username.getText().isBlank()
                    || password.getText().isBlank()) {
                error.setText("Логин мен құпиясөзді енгізіңіз!");
            } else {
                showChat(stage, username.getText().trim());
            }
        });

        VBox layout = new VBox(
                15, title, username, password, loginButton, error
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));

        stage.setScene(new Scene(layout, 420, 350));
    }

    // Чат терезесі
    private void showChat(Stage stage, String username) {
        Label title = new Label("Қауіпсіз корпоративтік чат");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label info = new Label("Демо нұсқа — серверге қосылмаған");

        ListView<String> messages = new ListView<>();

        TextField messageInput = new TextField();
        messageInput.setPromptText("Хабарлама жазыңыз...");

        Button sendButton = new Button("Жіберу");

        Runnable sendMessage = () -> {
            String message = messageInput.getText().trim();

            if (!message.isEmpty()) {
                messages.getItems().add(username + ": " + message);
                messageInput.clear();
            }
        };

        sendButton.setOnAction(event -> sendMessage.run());
        messageInput.setOnAction(event -> sendMessage.run());

        HBox inputBox = new HBox(10, messageInput, sendButton);
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        VBox layout = new VBox(
                15, title, info, messages, inputBox
        );

        layout.setPadding(new Insets(20));
        VBox.setVgrow(messages, Priority.ALWAYS);

        stage.setScene(new Scene(layout, 650, 480));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
