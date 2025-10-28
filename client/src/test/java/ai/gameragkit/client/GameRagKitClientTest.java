package ai.gameragkit.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameRagKitClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void askReturnsAnswerAndSendsAuthHeaders() throws Exception {
        server = startServer(exchange -> {
            assertThat(exchange.getRequestURI().getPath()).isEqualTo("/ask");
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isNull();
            assertThat(exchange.getRequestHeaders().getFirst("X-Api-Key")).isEqualTo("secret-key");
            assertThat(exchange.getRequestHeaders().getFirst("Content-Type")).isEqualTo("application/json");
            assertThat(exchange.getRequestHeaders().getFirst("X-GameRAG-Protocol")).isEqualTo("1");

            String response = "{\"text\":\"Hello\",\"sources\":[\"journal\"],\"scores\":[0.9],\"fromCloud\":false}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });

        URI baseUri = new URI("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort());

        try (GameRagKitClient client = new GameRagKitClient(baseUri, Duration.ofSeconds(5), "secret-key")) {
            Answer answer = client.ask("villager-1", "Hello?", AskOptions.defaults());
            assertThat(answer.text()).isEqualTo("Hello");
            assertThat(answer.sources()).containsExactly("journal");
            assertThat(answer.scores()).containsExactly(0.9);
            assertThat(answer.fromCloud()).isFalse();
        }
    }

    @Test
    void askSendsBearerTokenWhenPrefixed() throws Exception {
        server = startServer(exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer abc123");
            assertThat(exchange.getRequestHeaders().getFirst("X-Api-Key")).isNull();

            String response = "{\"text\":\"Hi\",\"sources\":[],\"scores\":[],\"fromCloud\":true}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });

        URI baseUri = new URI("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort());

        try (GameRagKitClient client = new GameRagKitClient(baseUri, Duration.ofSeconds(5), "Bearer abc123")) {
            Answer answer = client.ask("villager-1", "Hello?", AskOptions.defaults());
            assertThat(answer.fromCloud()).isTrue();
        }
    }

    @Test
    void askStreamPublishesTokens() throws Exception {
        server = startServer(new StreamHandler());

        URI baseUri = new URI("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort());

        try (GameRagKitClient client = new GameRagKitClient(baseUri, Duration.ofSeconds(5))) {
            Flow.Publisher<String> publisher = client.askStream("villager-2", "Tell me a story", null);
            List<String> received = new CopyOnWriteArrayList<>();
            CountDownLatch completed = new CountDownLatch(1);

            publisher.subscribe(new Flow.Subscriber<>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(String item) {
                    received.add(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    completed.countDown();
                    Assertions.fail("Stream failed", throwable);
                }

                @Override
                public void onComplete() {
                    completed.countDown();
                }
            });

            assertThat(completed.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(received).containsExactly("Hello", "traveler");
        }
    }

    @Test
    void askThrowsForServerBusy() throws Exception {
        server = startServer(exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        URI baseUri = new URI("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort());

        try (GameRagKitClient client = new GameRagKitClient(baseUri, Duration.ofSeconds(5))) {
            assertThatThrownBy(() -> client.ask("villager", "hi", AskOptions.defaults()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Server busy");
        }
    }

    @Test
    void askSupportsBaseUrlWithPath() throws Exception {
        HttpHandler askHandler = exchange -> {
            assertThat(exchange.getRequestURI().getPath()).isEqualTo("/api/ask");
            byte[] bytes = "{\"text\":\"Hi\",\"sources\":[],\"scores\":[],\"fromCloud\":false}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        };
        HttpHandler streamHandler = exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        };
        server = startServer("/api", askHandler, streamHandler);

        URI baseUri = new URI("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + "/api");

        try (GameRagKitClient client = new GameRagKitClient(baseUri, Duration.ofSeconds(5))) {
            Answer answer = client.ask("villager", "hi", null);
            assertThat(answer.text()).isEqualTo("Hi");
        }
    }

    private HttpServer startServer(HttpHandler askHandler) throws IOException {
        return startServer("", askHandler, askHandler instanceof StreamHandler streamHandler
                ? streamHandler
                : exchange -> {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                });
    }

    private HttpServer startServer(String basePath, HttpHandler askHandler, HttpHandler streamHandler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        String normalizedBase = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        String askPath = normalizedBase + "/ask";
        String streamPath = normalizedBase + "/ask/stream";
        httpServer.createContext(askPath, askHandler);
        httpServer.createContext(streamPath, streamHandler);
        httpServer.start();
        return httpServer;
    }

    private static final class StreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (Objects.equals(exchange.getRequestMethod(), "POST") && exchange.getRequestURI().getPath().endsWith("/ask/stream")) {
                exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 0);
                try (var out = exchange.getResponseBody()) {
                    out.write("data: Hello\n".getBytes(StandardCharsets.UTF_8));
                    out.write("data: traveler\n\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
                exchange.close();
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        }
    }
}
