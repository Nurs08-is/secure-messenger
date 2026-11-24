package kz.securemessenger.client;

import com.fasterxml.jackson.databind.JsonNode;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public class HrController {

    private final ApiClient api;

    private final ListView<UserItem> users =
            new ListView<>();

    private final Label status = new Label();

    private Runnable onBack;

    public HrController(ApiClient api) {
        this.api = api;
    }

    public Parent createView(Runnable onBack) {

        this.onBack = onBack;

        Label title = new Label("HR басқару панелі");
        title.getStyleClass().add("title");

        Label subtitle = new Label(
                "Аккаунттарды растау және бөлімдерді басқару");

        Button refresh = new Button("Жаңарту");
        Button approve = new Button("Растау");
        Button department = new Button("Бөлім құру");
        Button assign = new Button("Бөлімге бекіту");
        Button back = new Button("Артқа");

        refresh.setOnAction(event -> loadUsers());

        approve.setOnAction(event -> approveUser());

        department.setOnAction(event ->
                createDepartment());

        assign.setOnAction(event ->
                assignDepartment());

        back.setOnAction(event -> onBack.run());

        HBox actions = new HBox(
                12, refresh, approve,
                department, assign);

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
                () -> {

                    List<UserItem> result =
                            new ArrayList<>();

                    for (JsonNode node :
                            api.get("/api/hr/pending")) {

                        result.add(new UserItem(
                                node.path("id").asLong(),
                                node.path("username").asText(),
                                "PENDING"
                        ));
                    }

                    for (JsonNode node :
                            api.get("/api/users")) {

                        result.add(new UserItem(
                                node.path("id").asLong(),
                                node.path("username").asText(),
                                "ACTIVE"
                        ));
                    }

                    return result;
                },

                result -> {

                    users.getItems().setAll(result);

                    status.setText(
                            "Пайдаланушылар саны: "
                                    + result.size());
                }
        );
    }

    private void approveUser() {

        UserItem user = users
                .getSelectionModel()
                .getSelectedItem();

        if (user == null) {
            status.setText("Пайдаланушыны таңдаңыз");
            return;
        }

        if (!"PENDING".equals(user.status())) {
            status.setText("Аккаунт бұрын расталған");
            return;
        }

        runAsync(
                () -> api.post(
                        "/api/hr/approve/" + user.id(),
                        null
                ),

                response -> {

                    status.setText("Аккаунт расталды");

                    loadUsers();
                }
        );
    }

    private void createDepartment() {

        TextInputDialog dialog =
                new TextInputDialog();

        dialog.setTitle("Жаңа бөлім");

        dialog.setHeaderText(
                "Бөлімнің атауын енгізіңіз");

        dialog.showAndWait().ifPresent(name -> {

            if (name.isBlank()) {
                return;
            }

            runAsync(
                    () -> api.post(
                            "/api/hr/departments",
                            Map.of("name", name.trim())
                    ),

                    response -> status.setText(
                            "Бөлім сәтті құрылды")
            );
        });
    }

    private void assignDepartment() {

        UserItem user = users
                .getSelectionModel()
                .getSelectedItem();

        if (user == null) {
            status.setText("Пайдаланушыны таңдаңыз");
            return;
        }

        runAsync(
                () -> api.get("/api/hr/departments"),

                response -> {

                    List<DepartmentItem> departments =
                            new ArrayList<>();

                    for (JsonNode node : response) {

                        departments.add(
                                new DepartmentItem(
                                        node.path("id").asLong(),
                                        node.path("name").asText()
                                ));
                    }

                    if (departments.isEmpty()) {

                        status.setText(
                                "Алдымен бөлім құрыңыз");

                        return;
                    }

                    ChoiceDialog<DepartmentItem> dialog =
                            new ChoiceDialog<>(
                                    departments.get(0),
                                    departments
                            );

                    dialog.setTitle("Бөлімге бекіту");

                    dialog.setHeaderText(
                            user.username()
                                    + " үшін бөлім таңдаңыз");

                    dialog.showAndWait()
                            .ifPresent(department -> {

                        runAsync(
                                () -> api.put(
                                        "/api/hr/users/"
                                                + user.id()
                                                + "/department/"
                                                + department.id(),
                                        null
                                ),

                                result -> status.setText(
                                        "Қызметкер бөлімге бекітілді")
                        );
                    });
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
            String status) {

        @Override
        public String toString() {
            return username + " | " + status;
        }
    }

    private record DepartmentItem(
            long id,
            String name) {

        @Override
        public String toString() {
            return name;
        }
    }
}