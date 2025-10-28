package ai.gameragkit.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Minimal Java client for the GameRAGKit HTTP service.
 */
public final class GameRagKitClient implements AutoCloseable {
    private static final String PROTOCOL_HEADER = "X-GameRAG-Protocol";

    private final HttpClient http;
    private final URI askUri;
    private final URI streamUri;
    private final Duration requestTimeout;
    private final ObjectMapper json;
    private final String authorizationHeader;
    private final String apiKeyHeader;

    /**
     * Creates a client using anonymous access.
     *
     * @param baseUrl Base URL of the GameRAGKit server (e.g. {@code http://127.0.0.1:5280}).
     * @param timeout Request timeout for individual HTTP calls.
     */
    public GameRagKitClient(URI baseUrl, Duration timeout) {
        this(baseUrl, timeout, null);
    }

    /**
     * Creates a client with an optional API key or bearer token.
     *
     * @param baseUrl Base URL of the GameRAGKit server.
     * @param timeout Request timeout for individual HTTP calls.
     * @param apiKey  API key (sent as {@code X-Api-Key}) or bearer token (prefix with {@code Bearer }).
     */
    public GameRagKitClient(URI baseUrl, Duration timeout, String apiKey) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(timeout, "timeout");

        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
        URI normalizedBase = normalizeBaseUrl(baseUrl);
        this.askUri = normalizedBase.resolve("ask");
        this.streamUri = normalizedBase.resolve("ask/stream");
        this.requestTimeout = timeout;
        this.json = new ObjectMapper()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        if (apiKey != null && !apiKey.isBlank()) {
            var trimmed = apiKey.trim();
            if (trimmed.startsWith("Bearer ")) {
                this.authorizationHeader = trimmed;
                this.apiKeyHeader = null;
            } else {
                this.authorizationHeader = null;
                this.apiKeyHeader = trimmed;
            }
        } else {
            this.authorizationHeader = null;
            this.apiKeyHeader = null;
        }
    }

    /**
     * Sends a blocking {@code /ask} request.
     */
    public Answer ask(String npcId, String question, AskOptions options) throws IOException, InterruptedException {
        Objects.requireNonNull(npcId, "npcId");
        Objects.requireNonNull(question, "question");

        AskRequest payload = new AskRequest(npcId, question, options == null ? AskOptions.defaults() : options);
        String body = json.writeValueAsString(payload);

        HttpRequest.Builder builder = HttpRequest.newBuilder(askUri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header(PROTOCOL_HEADER, "1")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        applyAuthHeaders(builder);

        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        if (status == 429 || status >= 500) {
            throw new RuntimeException("Server busy: " + status);
        }
        if (status >= 400) {
            throw new RuntimeException("Request failed: " + status + " " + response.body());
        }

        return json.readValue(response.body(), Answer.class);
    }

    /**
     * Opens a server-sent events stream that emits answer chunks progressively.
     */
    public Flow.Publisher<String> askStream(String npcId, String question, AskOptions options) {
        Objects.requireNonNull(npcId, "npcId");
        Objects.requireNonNull(question, "question");

        AskRequest payload = new AskRequest(npcId, question, options == null ? AskOptions.defaults() : options);
        final String body;
        try {
            body = json.writeValueAsString(payload);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialise request", e);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(streamUri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header(PROTOCOL_HEADER, "1")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        applyAuthHeaders(builder);

        return subscriber -> {
            SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
            publisher.subscribe(subscriber);

            http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
                    .whenComplete((response, throwable) -> {
                        if (throwable != null) {
                            publisher.closeExceptionally(throwable);
                            return;
                        }
                        if (response.statusCode() >= 400) {
                            publisher.closeExceptionally(new RuntimeException("SSE failed: " + response.statusCode()));
                            return;
                        }
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                            String line;
                            StringBuilder dataBuffer = new StringBuilder();
                            while ((line = reader.readLine()) != null) {
                                if (line.isEmpty()) {
                                    if (dataBuffer.length() == 0) {
                                        continue;
                                    }
                                    String eventData = dataBuffer.toString();
                                    dataBuffer.setLength(0);
                                    if ("[DONE]".equals(eventData)) {
                                        publisher.close();
                                        return;
                                    }
                                    publishChunks(publisher, eventData);
                                    continue;
                                }

                                if (line.startsWith(":")) {
                                    // comment line; ignore
                                    continue;
                                }

                                if (line.startsWith("data:")) {
                                    if (dataBuffer.length() > 0) {
                                        dataBuffer.append('\n');
                                    }
                                    dataBuffer.append(line.substring(5).stripLeading());
                                }
                            }

                            if (dataBuffer.length() > 0) {
                                String eventData = dataBuffer.toString();
                                if (!"[DONE]".equals(eventData)) {
                                    publishChunks(publisher, eventData);
                                }
                            }
                            publisher.close();
                        } catch (Exception ex) {
                            publisher.closeExceptionally(ex);
                        }
                    });
        };
    }

    @Override
    public void close() {
        // HttpClient maintains its own shared resources; nothing explicit to close.
    }

    private void applyAuthHeaders(HttpRequest.Builder builder) {
        if (authorizationHeader != null) {
            builder.header("Authorization", authorizationHeader);
        }
        if (apiKeyHeader != null) {
            builder.header("X-Api-Key", apiKeyHeader);
        }
    }

    private static void publishChunks(SubmissionPublisher<String> publisher, String eventData) {
        if (eventData.isEmpty()) {
            return;
        }
        String[] pieces = eventData.split("\\n");
        for (String piece : pieces) {
            String chunk = piece.trim();
            if (!chunk.isEmpty()) {
                publisher.submit(chunk);
            }
        }
    }

    private static URI normalizeBaseUrl(URI baseUrl) {
        String path = baseUrl.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        } else if (!path.endsWith("/")) {
            path = path + "/";
        }
        try {
            return new URI(
                    baseUrl.getScheme(),
                    baseUrl.getAuthority(),
                    path,
                    baseUrl.getQuery(),
                    baseUrl.getFragment());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid base URL", ex);
        }
    }

    record AskRequest(String npc, String question, AskOptions options) {
    }
}
