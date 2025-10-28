# Plain Java Example

A console application that demonstrates the blocking and streaming APIs from the `gameragkit-client` library.

## Run it

```bash
export GAMERAG_URL=http://127.0.0.1:5280
export GAMERAG_API_KEY=""
./gradlew :examples:java-plain:run
```

The sample performs a blocking `ask()` call and then prints tokens from `askStream()` as they arrive. Override the environment variables to point at your GameRAGKit server and API key.
