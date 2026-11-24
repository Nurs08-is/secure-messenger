package kz.securemessenger.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class WebSocketClient {

    private final ApiClient api;
    private final SessionManager session;

    private final ObjectMapper mapper =
            new ObjectMapper();

    private volatile WebSocket socket;
    private volatile boolean closed;

    public WebSocketClient(
            ApiClient api,
            SessionManager session) {

        this.api = api;
        this.session = session;
    }

    public void connect(
            Consumer<Long> onMessage,
            Consumer<String> onError) {

        String token = session.getToken();

        if (token == null || token.isBlank()) {
            onError.accept("JWT token required");
            return;
        }

        closed = false;

        String url = api.getBaseUrl()
                .replaceFirst("^https://", "wss://")
                .replaceFirst("^http://", "ws://");

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .subprotocols("v12.stomp")
                .buildAsync(
                        URI.create(url + "/ws"),
                        new WebSocket.Listener() {

                    private final StringBuilder buffer =
                            new StringBuilder();

                    @Override
                    public void onOpen(WebSocket ws) {

                        if (closed) {
                            ws.abort();
                            return;
                        }

                        socket = ws;

                        ws.request(1);

                        ws.sendText(
                                "CONNECT\n"
                                + "accept-version:1.2\n"
                                + "host:localhost\n"
                                + "heart-beat:0,0\n"
                                + "Authorization:Bearer "
                                + token + "\n\n\0",
                                true
                        );
                    }

                    @Override
                    public CompletionStage<?> onText(
                            WebSocket ws,
                            CharSequence data,
                            boolean last) {

                        buffer.append(data);

                        int end;

                        while ((end = buffer.indexOf("\0")) >= 0) {

                            String frame =
                                    buffer.substring(0, end)
                                            .stripLeading();

                            buffer.delete(0, end + 1);

                            handleFrame(
                                    ws, frame,
                                    onMessage, onError);
                        }

                        ws.request(1);

                        return null;
                    }

                    @Override
                    public void onError(
                            WebSocket ws,
                            Throwable error) {

                        if (!closed) {
                            onError.accept(
                                    "WebSocket connection error");
                        }
                    }

                    @Override
                    public CompletionStage<?> onClose(
                            WebSocket ws,
                            int statusCode,
                            String reason) {

                        if (!closed) {
                            onError.accept(
                                    "WebSocket disconnected");
                        }

                        return null;
                    }

                }).exceptionally(error -> {

                    if (!closed) {
                        onError.accept(
                                "Cannot connect to WebSocket");
                    }

                    return null;
                });
    }

    private void handleFrame(
            WebSocket ws,
            String frame,
            Consumer<Long> onMessage,
            Consumer<String> onError) {

        if (closed) {
            return;
        }

        if (frame.startsWith("CONNECTED")) {

            ws.sendText(
                    "SUBSCRIBE\n"
                    + "id:messages-1\n"
                    + "destination:/user/queue/messages\n"
                    + "ack:auto\n\n\0",
                    true
            );

        } else if (frame.startsWith("MESSAGE")) {

            int start = frame.indexOf("\n\n");

            if (start < 0) {
                return;
            }

            try {

                String body = frame.substring(start + 2);

                var json = mapper.readTree(body);

                if ("NEW_MESSAGE".equals(
                        json.path("type").asText())) {

                    long chatId =
                            json.path("chatId").asLong();

                    if (chatId > 0) {
                        onMessage.accept(chatId);
                    }
                }

            } catch (Exception e) {
                onError.accept("Invalid notification");
            }

        } else if (frame.startsWith("ERROR")) {

            onError.accept("WebSocket authorization failed");
            ws.abort();
        }
    }

    public void close() {

        closed = true;

        WebSocket current = socket;

        socket = null;

        if (current != null) {
            current.abort();
        }
    }
}