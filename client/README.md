# GameRAGKit Java Client

A tiny Java 17 SDK that speaks to the GameRAGKit HTTP service. It exposes a blocking `ask` call and an SSE-based `askStream` publisher for typewriter-style responses.

## Installation

```kotlin
// build.gradle.kts
implementation("ai.gameragkit:gameragkit-client:1.0.0")
```

The library requires Java 17 or newer.

## Usage

```java
import ai.gameragkit.client.*;

var client = new GameRagKitClient(URI.create("http://127.0.0.1:5280"), Duration.ofSeconds(10));
Answer answer = client.ask("blacksmith", "Do you have any swords?", AskOptions.defaults());
System.out.println(answer.text());
```

For streaming tokens:

```java
client.askStream("blacksmith", "Tell me a story", AskOptions.defaults())
    .subscribe(new Flow.Subscriber<>() {
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override public void onNext(String item) { System.out.print(item); }
        @Override public void onError(Throwable throwable) { throwable.printStackTrace(); }
        @Override public void onComplete() { System.out.println(); }
    });
```

The client automatically adds the `X-GameRAG-Protocol: 1` header. Pass a raw key to send it via `X-Api-Key`, or prefix the value with `Bearer ` to opt into an `Authorization` header.

## Publishing

The module ships with Gradle `maven-publish` metadata. Configure the following environment variables or Gradle properties before running `./gradlew publish`:

- `OSSRH_USERNAME` / `OSSRH_PASSWORD`
- `SIGNING_KEY` / `SIGNING_PASSWORD`

Publishing is typically done through the GitHub Actions workflow in this repository.
