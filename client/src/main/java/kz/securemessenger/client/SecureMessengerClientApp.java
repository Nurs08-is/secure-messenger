package kz.securemessenger.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SecureMessengerClientApp extends Application {

    private Stage stage;

    private final SessionManager session =
            new SessionManager();

    private final ApiClient api =
            new ApiClient(session);

    private ChatController chatController;

    @Override
    public void start(Stage stage) {

        this.stage = stage;

        stage.setTitle("Secure Corporate Messenger");

        stage.setMinWidth(760);
        stage.setMinHeight(520);

        stage.setOnCloseRequest(event -> {
            closeChat();
            session.logout();
        });

        showLogin();

        stage.show();
    }

    private void showLogin() {

        closeChat();
        session.logout();

        LoginController controller =
                new LoginController(api, session);

        setView(controller.createView(
                this::showDashboard,
                this::showRegister
        ));
    }

    private void showRegister() {

        RegisterController controller =
                new RegisterController(api);

        setView(controller.createView(
                this::showLogin
        ));
    }

    private void showDashboard() {

        closeChat();

        if (!session.isLoggedIn()) {
            showLogin();
            return;
        }

        Label title = new Label("Secure Messenger");
        title.getStyleClass().add("title");

        Label welcome = new Label(
                "Қош келдіңіз, "
                        + session.getUsername());

        Label role = new Label(
                "Сіздің рөліңіз: "
                        + session.getRole());

        VBox root = new VBox(18);

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("auth-page");

        root.getChildren().addAll(
                title, welcome, role);

        if (!session.hasRole("AUDITOR")
                && session.getKeyPair() != null) {

            root.getChildren().add(
                    menuButton(
                            "Хабарламалар",
                            this::showChat));
        }

        if (session.hasRole("ADMIN")) {

            root.getChildren().add(
                    menuButton(
                            "Әкімші панелі",
                            this::showAdmin));
        }

        if (session.hasRole("HR")) {

            root.getChildren().add(
                    menuButton(
                            "HR басқару панелі",
                            this::showHr));
        }

        if (session.hasRole("AUDITOR")) {

            root.getChildren().add(
                    menuButton(
                            "Аудит журналы",
                            this::showAudit));
        }

        if (session.hasRole("MANAGER")) {

            Label info = new Label(
                    "Бөлім чаттарын хабарламалар "
                            + "бөлімінен басқара аласыз");

            info.setWrapText(true);

            root.getChildren().add(info);
        }

        root.getChildren().add(
                menuButton(
                        "Аккаунттан шығу",
                        this::showLogin));

        setView(root);
    }

    private void showChat() {

        closeChat();

        chatController =
                new ChatController(api, session);

        setView(chatController.createView(
                this::showLogin
        ));
    }

    private void showAdmin() {

        AdminController controller =
                new AdminController(api);

        setView(controller.createView(
                this::showDashboard
        ));
    }

    private void showHr() {

        HrController controller =
                new HrController(api);

        setView(controller.createView(
                this::showDashboard
        ));
    }

    private void showAudit() {

        AuditController controller =
                new AuditController(api);

        setView(controller.createView(
                this::showDashboard
        ));
    }

    private Button menuButton(
            String text,
            Runnable action) {

        Button button = new Button(text);

        button.setPrefWidth(320);

        button.setOnAction(event ->
                action.run());

        return button;
    }

    private void setView(Parent root) {

        if (stage.getScene() == null) {

            Scene scene = new Scene(
                    root, 1100, 720);

            var css = getClass()
                    .getResource("/style.css");

            if (css != null) {

                scene.getStylesheets().add(
                        css.toExternalForm());
            }

            stage.setScene(scene);

        } else {

            stage.getScene().setRoot(root);
        }
    }

    private void closeChat() {

        if (chatController != null) {

            chatController.close();

            chatController = null;
        }
    }

    @Override
    public void stop() {

        closeChat();
        session.logout();
    }

    public static void main(String[] args) {
        launch(args);
    }
}