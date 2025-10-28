# Paper Demo Plugin

A minimal Paper plugin that calls the GameRAGKit HTTP service whenever a player right-clicks a villager. The request runs off the main thread and the answer is sent back to the player chat.

## Build

```bash
./gradlew :mc-sample-paper:shadowJar -PuseRealPaperApi=true
```

The repo ships with a lightweight stub of the Bukkit API so offline builds succeed. Pass `-PuseRealPaperApi=true` to resolve the real Paper dependency when preparing a production plugin.

## Configure

Copy `config.yml` to your server's plugin configuration directory and adjust:

```yaml
gamerag:
  url: "http://127.0.0.1:5280"
  apiKey: ""
```

Restart the server and talk to a villager to trigger a call to `ask()`.
