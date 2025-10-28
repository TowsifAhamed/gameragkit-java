package ai.gameragkit.examples.plain;

import ai.gameragkit.client.Answer;
import ai.gameragkit.client.AskOptions;
import ai.gameragkit.client.GameRagKitClient;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Minimal console example that demonstrates both blocking and streaming calls.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        URI baseUri = URI.create(System.getenv().getOrDefault("GAMERAG_URL", "http://127.0.0.1:5280"));
        String apiKey = System.getenv().getOrDefault("GAMERAG_API_KEY", "");

        GameRagKitClient client = apiKey.isBlank()
                ? new GameRagKitClient(baseUri, Duration.ofSeconds(20))
                : new GameRagKitClient(baseUri, Duration.ofSeconds(20), apiKey);

        try (client) {
            System.out.println("=== Blocking ask() ===");
            Answer answer = client.ask("demo-npc", "Hello there!", AskOptions.defaults());
            System.out.printf(Locale.US, "NPC replied (cloud=%s): %s%n", answer.fromCloud(), answer.text());

            System.out.println();
            System.out.println("=== Streaming askStream() ===");
            CountDownLatch done = new CountDownLatch(1);

            Flow.Publisher<String> publisher = client.askStream("demo-npc", "Tell me a tale of the town.", AskOptions.defaults());
            publisher.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(String item) {
                    System.out.print(item);
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Stream failed: " + throwable.getMessage());
                    done.countDown();
                }

                @Override
                public void onComplete() {
                    System.out.println();
                    done.countDown();
                }
            });

            done.await(30, TimeUnit.SECONDS);
        }
    }
}
