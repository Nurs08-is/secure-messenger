package kz.securemessenger.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {

    private final String baseUrl =
            System.getProperty(
                    "messenger.server.url",
                    "http://localhost:8080");

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

    private final ObjectMapper mapper =
            new ObjectMapper();

    private final SessionManager session;

    public ApiClient(SessionManager session) {
        this.session = session;
    }

    public JsonNode get(String path) throws Exception {
        return request("GET", path, null);
    }

    public JsonNode post(String path, Object body)
            throws Exception {

        return request("POST", path, body);
    }

    public JsonNode put(String path, Object body)
            throws Exception {

        return request("PUT", path, body);
    }

    public JsonNode delete(String path) throws Exception {
        return request("DELETE", path, null);
    }

    private JsonNode request(
            String method,
            String path,
            Object body) throws Exception {

        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(20))
                        .header("Accept", "application/json");

        String token = session.getToken();

        if (token != null && !token.isBlank()) {

            builder.header(
                    "Authorization",
                    "Bearer " + token
            );
        }

        if (body != null) {

            builder.header(
                    "Content-Type",
                    "application/json"
            );

            builder.method(
                    method,
                    HttpRequest.BodyPublishers.ofString(
                            mapper.writeValueAsString(body))
            );

        } else {

            builder.method(
                    method,
                    HttpRequest.BodyPublishers.noBody()
            );
        }

        HttpResponse<String> response =
                httpClient.send(
                        builder.build(),
                        HttpResponse.BodyHandlers.ofString()
                );

        int status = response.statusCode();

        if (status == 401) {
            session.logout();
        }

        if (status < 200 || status >= 300) {

            String error = "Server error: " + status;

            try {

                JsonNode json =
                        mapper.readTree(response.body());

                if (json != null && json.has("error")) {
                    error = json.get("error").asText();
                }

            } catch (Exception ignored) {
            }

            throw new ApiException(status, error);
        }

        if (response.body().isBlank()) {
            return mapper.createObjectNode();
        }

        return mapper.readTree(response.body());
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public static class ApiException extends Exception {

        private final int status;

        public ApiException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}