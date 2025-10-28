package com.example.mcrag;

import ai.gameragkit.client.AskOptions;
import ai.gameragkit.client.GameRagKitClient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.time.Duration;

/**
 * Demonstrates how to call the GameRAGKit service from a Paper plugin without blocking the main thread.
 */
public final class MCGameRagPlugin extends JavaPlugin implements Listener {
    private GameRagKitClient client;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        var url = URI.create(getConfig().getString("gamerag.url"));
        var apiKey = getConfig().getString("gamerag.apiKey", "");

        client = apiKey == null || apiKey.isBlank()
                ? new GameRagKitClient(url, Duration.ofSeconds(20))
                : new GameRagKitClient(url, Duration.ofSeconds(20), apiKey);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("GameRAGKit client ready at " + url);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTalk(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        var npcId = "villager-" + villager.getUniqueId();
        var player = event.getPlayer();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                var answer = client.ask(npcId, "Hello there!", AskOptions.defaults());
                Bukkit.getScheduler().runTask(this, () ->
                        player.sendMessage("§a" + answer.text()));
            } catch (Exception ex) {
                Bukkit.getScheduler().runTask(this, () ->
                        player.sendMessage("§c[GameRAG] " + ex.getMessage()));
            }
        });
    }

    @Override
    public void onDisable() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
                // HttpClient does not require explicit cleanup, but we guard just in case.
            }
        }
    }
}
