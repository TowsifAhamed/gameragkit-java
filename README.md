# GameRAGKit for Java & Minecraft

This repository delivers a lean Java 17 client SDK plus runnable samples so Minecraft server owners can use [GameRAGKit](https://github.com/TowsifAhamed/GameRAGKit) without touching .NET.

## Modules

```
client/             → Java 17 library published to Maven Central
mc-sample-paper/    → Paper plugin that calls GameRAGKit off the main thread
examples/java-plain/→ Console example for quick sanity checks
stubs/paper-api-stubs/ → Minimal Bukkit API surface used for offline builds
```

## Quick start (Java)

```java
import ai.gameragkit.client.*;

try (var client = new GameRagKitClient(URI.create("http://127.0.0.1:5280"), Duration.ofSeconds(15))) {
    Answer answer = client.ask("blacksmith", "Do you repair swords?", AskOptions.defaults());
    System.out.println(answer.text());
}
```

Gradle dependency:

```kotlin
dependencies {
    implementation("ai.gameragkit:gameragkit-client:1.0.0")
}
```

The SDK uses Java's built-in `HttpClient`, automatically adds `X-GameRAG-Protocol: 1`, ignores unknown JSON properties, and supports optional auth. Pass a plain key to send `X-Api-Key`, or prefix it with `Bearer ` to emit an `Authorization` header instead.

## Streaming tokens

Use `askStream` for SSE token streaming:

```java
client.askStream("bard", "Sing me a song", AskOptions.defaults())
      .subscribe(new Flow.Subscriber<>() {
          @Override public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
          @Override public void onNext(String token) { System.out.print(token); }
          @Override public void onError(Throwable t) { t.printStackTrace(); }
          @Override public void onComplete() { System.out.println(); }
      });
```

## Minecraft (Paper) demo

```bash
./gradlew :mc-sample-paper:shadowJar -PuseRealPaperApi=true
```

By default the module compiles against a minimal stub of the Bukkit API so the repository builds offline. Pass `-PuseRealPaperApi=true` when you want to resolve the actual Paper dependency.

Drop the produced jar into your Paper server's `plugins/` folder. Configure `plugins/MCGameRagDemo/config.yml`:

```yaml
gamerag:
  url: "http://127.0.0.1:5280"
  apiKey: ""
```

The plugin schedules `ask()` on an async thread and hops back to the main thread for Bukkit API calls. Extend it with streaming to push tokens progressively to players.

## Development

- Requires Java 17+
- `./gradlew :client:check` runs unit tests for the client module
- `./gradlew :examples:java-plain:run` executes the console demo
- The Gradle wrapper JAR lives as `gradle-wrapper.jar.base64` to keep pull requests text-only; the first wrapper invocation reconstructs `gradle/wrapper/gradle-wrapper.jar` automatically.

## CI & Publishing

`.github/workflows/release.yml` builds and tests the project, then publishes the `client` module to Maven Central when credentials are present (`OSSRH_USERNAME`, `OSSRH_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD`).

## License

Dual-licensed: [PolyForm Noncommercial 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0/) for community use and a commercial license available from Towsif Ahamed (<towsif.kuet.ac.bd@gmail.com>).
