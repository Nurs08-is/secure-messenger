package kz.securemessenger.client;

import com.fasterxml.jackson.databind.JsonNode;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public class AdminController {

    private final ApiClient api;

    private final ListView<UserItem> users =
            new ListView<>();

    private final Label status = new Label();

    private Runnable onBack;

    public AdminController(ApiClient api) {
        this.api = api;
    }

    public Parent createView(Runnable onBack) {

        this.onBack = onBack;

        Label title = new Label("Әкімші панелі");
        title.getStyleClass().add("title");

        Label subtitle = new Label(
                "Пайдаланушылар мен рөлдерді басқару");

        Button refresh = new Button("Жаңарту");
        Button role = new Button("Рөлді өзгерту");
        Button block = new Button("Бұғаттау");
        Button unblock = new Button("Бұғаттан шығару");
        Button back = new Button("Артқа");

        refresh.setOnAction(event -> loadUsers());

        role.setOnAction(event -> changeRole());

        block.setOnAction(event ->
                changeStatus(true));

        unblock.setOnAction(event ->
                changeStatus(false));

        back.setOnAction(event -> onBack.run());

        HBox actions = new HBox(
                12, refresh, role, block, unblock);

        VBox root = new VBox(
                18,
                title,
                subtitle,
                users,
                actions,
                status,
                back
        );

        root.setPadding(new Insets(30));
        root.getStyleClass().add("admin-page");

        VBox.setVgrow(users, Priority.ALWAYS);

        loadUsers();

        return root;
    }

    private void loadUsers() {

        runAsync(
                () -> api.get("/api/admin/users"),

                response -> {

                    users.getItems().clear();

                    for (JsonNode node : response) {

                        users.getItems().add(
                                new UserItem(
                                        node.path("id").asLong(),
                                        node.path("username").asText(),
                                        node.path("role").asText(),
                                        node.path("status").asText()
                                ));
                    }

                    status.setText(
                            "Пайдаланушылар саны: "
                                    + users.getItems().size());
                }
        );
    }

    private void changeRole() {

        UserItem user = users
                .getSelectionModel()
                .getSelectedItem();

        if (user == null) {
            status.setText("Пайдаланушыны таңдаңыз");
            return;
        }

        ChoiceDialog<String> dialog =
                new ChoiceDialog<>(
                        user.role(),
                        List.of(
                                "EMPLOYEE",
                                "MANAGER",
                                "HR",
                                "ADMIN",
                                "AUDITOR"
                        ));

        dialog.setTitle("Рөлді өзгерту");

        dialog.setHeaderText(
                user.username() + " үшін жаңа рөл");

        dialog.showAndWait().ifPresent(role -> {

            runAsync(
                    () -> api.put(
                            "/api/admin/users/"
                                    + user.id() + "/role",

                            Map.of("role", role)
                    ),

                    response -> {
                        status.setText("Рөл өзгертілді");
                        loadUsers();
                    }
            );
        });
    }

    private void changeStatus(boolean blocking) {

        UserItem user = users
                .getSelectionModel()
                .getSelectedItem();

        if (user == null) {
            status.setText("Пайдаланушыны таңдаңыз");
            return;
        }

        String action = blocking
                ? "block"
                : "unblock";

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION);

        confirm.setTitle("Әрекетті растау");

        confirm.setHeaderText(
                user.username() + ": "
                        + (blocking
                            ? "Бұғаттау"
                            : "Бұғаттан шығару"));

        if (confirm.showAndWait()
                .orElse(ButtonType.CANCEL)
                != ButtonType.OK) {
            return;
        }

        runAsync(
                () -> api.put(
                        "/api/admin/users/"
                                + user.id()
                                + "/" + action,
                        null
                ),

                response -> {

                    status.setText(
                            blocking
                                    ? "Аккаунт бұғатталды"
                                    : "Аккаунт ашылды");

                    loadUsers();
                }
        );
    }

    private <T> void runAsync(
            CheckedTask<T> task,
            Consumer<T> success) {

        CompletableFuture.supplyAsync(() -> {

            try {
                return task.run();

            } catch (Exception e) {
                throw new CompletionException(e);
            }

        }).whenComplete((result, error) ->

                Platform.runLater(() -> {

                    if (error == null) {

                        success.accept(result);

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

    @FunctionalInterface
    private interface CheckedTask<T> {
        T run() throws Exception;
    }

    private record UserItem(
            long id,
            String username,
            String role,
            String status) {

        @Override
        public String toString() {

            return username
                    + "  |  " + role
                    + "  |  " + status;
        }
    }
}