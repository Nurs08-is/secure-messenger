package kz.securemessenger.client;

import com.fasterxml.jackson.databind.JsonNode;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AuditController {

    private final ApiClient api;

    private final ListView<String> logs =
            new ListView<>();

    private final Label status = new Label();

    private Runnable onBack;

    public AuditController(ApiClient api) {
        this.api = api;
    }

    public Parent createView(Runnable onBack) {

        this.onBack = onBack;

        Label title = new Label("Аудит журналы");
        title.getStyleClass().add("title");

        Label subtitle = new Label(
                "Жүйедегі пайдаланушы әрекеттерін бақылау");

        TextField username = new TextField();
        username.setPromptText("Пайдаланушы логині");

        ComboBox<String> action = new ComboBox<>();

        action.getItems().addAll(
                "ALL",
                "LOGIN_SUCCESS",
                "LOGIN_FAILED",
                "ACCOUNT_APPROVED",
                "DEPARTMENT_ASSIGNED",
                "ROLE_CHANGED",
                "ACCOUNT_BLOCKED",
                "ACCOUNT_UNBLOCKED",
                "CHAT_CREATED",
                "MEMBER_ADDED",
                "MEMBER_REMOVED"
        );

        action.setValue("ALL");

        Button search = new Button("Іздеу");
        Button refresh = new Button("Жаңарту");
        Button back = new Button("Артқа");

        search.setOnAction(event -> {

            String name = username.getText().trim();

            if (!name.isBlank()) {

                String encoded = URLEncoder.encode(
                        name, StandardCharsets.UTF_8)
                        .replace("+", "%20");

                loadLogs("/api/audit/user/" + encoded);

            } else if (!"ALL".equals(action.getValue())) {

                loadLogs("/api/audit/action/"
                        + action.getValue());

            } else {
                loadLogs("/api/audit");
            }
        });

        refresh.setOnAction(event -> {

            username.clear();
            action.setValue("ALL");

            loadLogs("/api/audit");
        });

        back.setOnAction(event -> onBack.run());

        HBox filters = new HBox(
                12, username, action, search, refresh);

        HBox.setHgrow(username, Priority.ALWAYS);

        VBox root = new VBox(
                18,
                title,
                subtitle,
                filters,
                logs,
                status,
                back
        );

        root.setPadding(new Insets(30));
        root.getStyleClass().add("admin-page");

        VBox.setVgrow(logs, Priority.ALWAYS);

        loadLogs("/api/audit");

        return root;
    }

    private void loadLogs(String path) {

        status.setText("Жүктелуде...");

        CompletableFuture.supplyAsync(() -> {

            try {

                JsonNode response = api.get(path);

                List<String> result = new ArrayList<>();

                for (JsonNode node : response) {

                    result.add(
                            node.path("createdAt").asText()
                            + " | "
                            + node.path("username").asText()
                            + " | "
                            + node.path("action").asText()
                            + " | "
                            + node.path("details").asText()
                    );
                }

                return result;

            } catch (Exception e) {
                throw new CompletionException(e);
            }

        }).whenComplete((result, error) ->

                Platform.runLater(() -> {

                    if (error == null) {

                        logs.getItems().setAll(result);

                        status.setText(
                                "Жазбалар саны: "
                                        + result.size());

                    } else {

                        Throwable cause =
                                error.getCause() == null
                                        ? error
                                        : error.getCause();

                        if (cause instanceof ApiClient.ApiException ex
                                && ex.getStatus() == 401) {

                            onBack.run();

                        } else {

                            status.setText(
                                    "Қате: " + cause.getMessage());
                        }
                    }
                })
        );
    }
}