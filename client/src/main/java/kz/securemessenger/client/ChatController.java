package kz.securemessenger.client;

import kz.securemessenger.crypto.HybridCryptoService;
import kz.securemessenger.crypto.KeyUtils;

import com.fasterxml.jackson.databind.JsonNode;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.security.PublicKey;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public class ChatController {

    private final ApiClient api;
    private final SessionManager session;
    private final WebSocketClient webSocket;

    private final HybridCryptoService crypto =
            new HybridCryptoService();

    private final ListView<ChatItem> chats =
            new ListView<>();

    private final ListView<String> messages =
            new ListView<>();

    private final TextField input = new TextField();

    private final Label chatTitle =
            new Label("Чатты таңдаңыз");

    private final Label status = new Label();

    private final Button send = new Button("Жіберу");

    private Runnable onLogout;
    private boolean closed;
    private long historyVersion;

    public ChatController(
            ApiClient api,
            SessionManager session) {

        this.api = api;
        this.session = session;

        this.webSocket =
                new WebSocketClient(api, session);
    }

    public Parent createView(Runnable onLogout) {

        this.onLogout = onLogout;

        Label logo = new Label("Secure Messenger");
        logo.getStyleClass().add("title");

        Label profile = new Label(
                session.getUsername()
                        + " | " + session.getRole());

        Button logout = new Button("Шығу");
        logout.setOnAction(event -> logout());

        Button create = new Button("+ Жаңа чат");
        create.setMaxWidth(Double.MAX_VALUE);
        create.setOnAction(event -> createChat());

        Button refresh = new Button("Жаңарту");
        refresh.setMaxWidth(Double.MAX_VALUE);
        refresh.setOnAction(event -> refreshChats());

        VBox sidebar = new VBox(
                14, logo, profile, create,
                refresh, chats, logout);

        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(280);
        sidebar.getStyleClass().add("sidebar");

        VBox.setVgrow(chats, Priority.ALWAYS);

        Button members = new Button("Қатысушылар");
        members.setOnAction(event -> showMembers());

        Button add = new Button("+ Қосу");
        add.setOnAction(event -> changeMember(true));

        Button remove = new Button("Шығару");
        remove.setOnAction(event -> changeMember(false));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(
                12, chatTitle, spacer,
                members, add, remove);

        chatTitle.getStyleClass().add("title");

        input.setPromptText("Хабарлама жазыңыз...");

        send.setOnAction(event -> sendMessage());
        input.setOnAction(event -> sendMessage());

        HBox.setHgrow(input, Priority.ALWAYS);

        HBox composer = new HBox(12, input, send);

        VBox content = new VBox(
                16, header, messages, composer, status);

        content.setPadding(new Insets(24));
        content.getStyleClass().add("chat-content");

        VBox.setVgrow(messages, Priority.ALWAYS);

        chats.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, selected) -> {

                    historyVersion++;
                    messages.getItems().clear();

                    if (selected != null) {

                        chatTitle.setText(selected.name());
                        loadMessages(selected.id());

                    } else {
                        chatTitle.setText("Чатты таңдаңыз");
                    }

                    updateComposer();
                });

        BorderPane root = new BorderPane();

        root.setLeft(sidebar);
        root.setCenter(content);
        root.getStyleClass().add("messenger");

        updateComposer();
        refreshChats();

        webSocket.connect(
                chatId -> Platform.runLater(() -> {

                    if (closed) {
                        return;
                    }

                    ChatItem selected = chats
                            .getSelectionModel()
                            .getSelectedItem();

                    if (selected != null &&
                            selected.id() == chatId) {

                        loadMessages(chatId);

                    } else {
                        refreshChats();
                    }

                }),

                error -> Platform.runLater(() -> {
                    if (!closed) {
                        status.setText(error);
                    }
                })
        );

        return root;
    }

    private void refreshChats() {

        runAsync(
                () -> api.get("/api/chats"),
                response -> {

                    ChatItem previous = chats
                            .getSelectionModel()
                            .getSelectedItem();

                    List<ChatItem> result =
                            new ArrayList<>();

                    for (JsonNode node : response) {

                        result.add(new ChatItem(
                                node.path("id").asLong(),
                                node.path("name").asText(),
                                node.path("type").asText()
                        ));
                    }

                    chats.getItems().setAll(result);

                    if (previous != null) {

                        result.stream()
                                .filter(chat ->
                                        chat.id() == previous.id())
                                .findFirst()
                                .ifPresent(chat ->
                                        chats.getSelectionModel()
                                                .select(chat));
                    }

                    if (chats.getSelectionModel()
                            .getSelectedItem() == null &&
                            !result.isEmpty()) {

                        chats.getSelectionModel().selectFirst();
                    }
                }
        );
    }

    private void createChat() {

        if (session.getKeyPair() == null) {
            status.setText("RSA кілті қажет");
            return;
        }

        List<String> types = new ArrayList<>(
                List.of("PRIVATE", "GROUP"));

        if (session.hasRole("MANAGER")) {
            types.add("DEPARTMENT");
        }

        ChoiceDialog<String> typeDialog =
                new ChoiceDialog<>("PRIVATE", types);

        typeDialog.setTitle("Жаңа чат");
        typeDialog.setHeaderText("Чат түрін таңдаңыз");

        typeDialog.showAndWait().ifPresent(type -> {

            TextInputDialog nameDialog =
                    new TextInputDialog();

            nameDialog.setHeaderText("Чат атауын енгізіңіз");

            nameDialog.showAndWait().ifPresent(name -> {

                if (name.isBlank()) {
                    return;
                }

                String path = type.equals("DEPARTMENT")
                        ? "/api/users/department"
                        : "/api/users";

                runAsync(
                        () -> api.get(path),
                        users -> {

                            List<UserItem> candidates =
                                    new ArrayList<>();

                            for (JsonNode user : users) {

                                UserItem item = toUser(user);

                                if (!item.username().equals(
                                        session.getUsername()) &&
                                        !item.publicKey().isBlank() &&
                                        !"AUDITOR".equals(
                                                user.path("role").asText())) {

                                    candidates.add(item);
                                }
                            }

                            chooseUsers(candidates).ifPresent(ids -> {

                                runAsync(
                                        () -> api.post(
                                                "/api/chats",
                                                Map.of(
                                                        "name", name.trim(),
                                                        "type", type,
                                                        "memberIds", ids
                                                )),
                                        result -> {
                                            status.setText("Чат құрылды");
                                            refreshChats();
                                        }
                                );
                            });
                        }
                );
            });
        });
    }

    private void loadMessages(long chatId) {

        long version = ++historyVersion;

        var pair = session.getKeyPair();

        runAsync(
                () -> {

                    JsonNode response = api.get(
                            "/api/chats/" + chatId + "/messages");

                    List<String> result = new ArrayList<>();

                    for (JsonNode node : response) {

                        String text;

                        try {

                            if (pair == null) {
                                throw new IllegalStateException();
                            }

                            text = crypto.decrypt(
                                    node.path("ciphertext").asText(),
                                    node.path("iv").asText(),
                                    node.path("encryptedKey").asText(),
                                    pair.getPrivate()
                            );

                        } catch (Exception e) {
                            text = "[Хабарламаны ашу мүмкін емес]";
                        }

                        result.add(
                                node.path("sender").asText()
                                        + ": " + text);
                    }

                    return result;
                },

                result -> {

                    if (version != historyVersion) {
                        return;
                    }

                    messages.getItems().setAll(result);

                    if (!result.isEmpty()) {
                        messages.scrollTo(result.size() - 1);
                    }
                }
        );
    }

    private void sendMessage() {

        ChatItem chat = chats
                .getSelectionModel()
                .getSelectedItem();

        String text = input.getText().trim();

        if (chat == null || text.isBlank() ||
                session.getKeyPair() == null) {
            return;
        }

        send.setDisable(true);

        long chatId = chat.id();

        runAsync(
                () -> {

                    JsonNode members = api.get(
                            "/api/chats/" + chatId + "/members");

                    Map<Long, PublicKey> recipients =
                            new HashMap<>();

                    for (JsonNode member : members) {

                        recipients.put(
                                member.path("id").asLong(),
                                KeyUtils.decodePublicKey(
                                        member.path("publicKey").asText())
                        );
                    }

                    var encrypted =
                            crypto.encrypt(text, recipients);

                    return api.post(
                            "/api/chats/" + chatId + "/messages",
                            encrypted
                    );
                },

                response -> {

                    if (input.getText().trim().equals(text)) {
                        input.clear();
                    }

                    status.setText("Хабарлама жіберілді");

                    updateComposer();

                    ChatItem selected = chats
                            .getSelectionModel()
                            .getSelectedItem();

                    if (selected != null &&
                            selected.id() == chatId) {

                        loadMessages(chatId);
                    }
                }
        );
    }

    private void showMembers() {

        ChatItem chat = chats
                .getSelectionModel()
                .getSelectedItem();

        if (chat == null) {
            return;
        }

        runAsync(
                () -> {

                    JsonNode response = api.get(
                            "/api/chats/" + chat.id() + "/members");

                    StringBuilder result = new StringBuilder();

                    for (JsonNode member : response) {

                        result.append(
                                member.path("username").asText());

                        String key =
                                member.path("publicKey").asText("");

                        if (!key.isBlank()) {

                            byte[] hash = MessageDigest
                                    .getInstance("SHA-256")
                                    .digest(KeyUtils.decodePublicKey(key)
                                            .getEncoded());

                            result.append("\nFingerprint: ")
                                    .append(HexFormat.of()
                                            .formatHex(hash));
                        }

                        result.append("\n\n");
                    }

                    return result.toString();
                },

                result -> {

                    TextArea area = new TextArea(result);
                    area.setEditable(false);
                    area.setWrapText(true);

                    Alert alert = new Alert(
                            Alert.AlertType.INFORMATION);

                    alert.setTitle("Қатысушылар");
                    alert.setHeaderText(
                            "Пайдаланушылар және RSA fingerprint");

                    alert.getDialogPane().setContent(area);
                    alert.showAndWait();
                }
        );
    }

    private void changeMember(boolean adding) {

        ChatItem chat = chats
                .getSelectionModel()
                .getSelectedItem();

        if (chat == null) {
            return;
        }

        if ("PRIVATE".equals(chat.type())) {
            status.setText(
                    "Жеке чат мүшелерін өзгертуге болмайды");
            return;
        }

        String path = adding
                ? ("DEPARTMENT".equals(chat.type())
                    ? "/api/users/department"
                    : "/api/users")
                : "/api/chats/" + chat.id() + "/members";

        runAsync(
                () -> api.get(path),
                response -> {

                    List<UserItem> users = new ArrayList<>();

                    for (JsonNode node : response) {

                        UserItem user = toUser(node);

                        if (!user.username().equals(
                                session.getUsername())) {

                            users.add(user);
                        }
                    }

                    if (users.isEmpty()) {
                        status.setText("Пайдаланушылар жоқ");
                        return;
                    }

                    ChoiceDialog<UserItem> dialog =
                            new ChoiceDialog<>(users.get(0), users);

                    dialog.setHeaderText(
                            adding
                                    ? "Қатысушыны қосу"
                                    : "Қатысушыны шығару");

                    dialog.showAndWait().ifPresent(user -> {

                        String url = "/api/chats/"
                                + chat.id()
                                + "/members/"
                                + user.id();

                        runAsync(
                                () -> adding
                                        ? api.post(url, null)
                                        : api.delete(url),

                                result -> status.setText(
                                        "Әрекет орындалды")
                        );
                    });
                }
        );
    }

    private Optional<List<Long>> chooseUsers(
            List<UserItem> users) {

        ListView<UserItem> list = new ListView<>();

        list.getItems().setAll(users);

        list.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);

        Dialog<List<Long>> dialog = new Dialog<>();

        dialog.setTitle("Қатысушылар");
        dialog.setHeaderText(
                "Бірнеше адам таңдау үшін Ctrl басыңыз");

        dialog.getDialogPane().setContent(list);

        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {

            if (button == ButtonType.OK) {

                return list.getSelectionModel()
                        .getSelectedItems()
                        .stream()
                        .map(UserItem::id)
                        .toList();
            }

            return null;
        });

        return dialog.showAndWait();
    }

    private UserItem toUser(JsonNode node) {

        return new UserItem(
                node.path("id").asLong(),
                node.path("username").asText(),
                node.path("publicKey").asText("")
        );
    }

    private void updateComposer() {

        boolean disabled =
                chats.getSelectionModel()
                        .getSelectedItem() == null ||
                session.getKeyPair() == null;

        send.setDisable(disabled);
        input.setDisable(disabled);
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

                    if (closed) {
                        return;
                    }

                    if (error == null) {

                        success.accept(result);

                    } else {

                        Throwable cause = error.getCause() == null
                                ? error
                                : error.getCause();

                        if (cause instanceof ApiClient.ApiException ex
                                && ex.getStatus() == 401) {

                            logout();

                        } else {

                            status.setText(
                                    "Қате: " + cause.getMessage());

                            updateComposer();
                        }
                    }
                })
        );
    }

    public void close() {
        closed = true;
        webSocket.close();
    }

    private void logout() {

        close();
        session.logout();
        onLogout.run();
    }

    @FunctionalInterface
    private interface CheckedTask<T> {
        T run() throws Exception;
    }

    private record ChatItem(
            long id,
            String name,
            String type) {

        @Override
        public String toString() {
            return name + " (" + type + ")";
        }
    }

    private record UserItem(
            long id,
            String username,
            String publicKey) {

        @Override
        public String toString() {
            return username;
        }
    }
}