package org.bukkit.scheduler;

import org.bukkit.plugin.java.JavaPlugin;

public interface BukkitScheduler {
    void runTaskAsynchronously(JavaPlugin plugin, Runnable runnable);

    void runTask(JavaPlugin plugin, Runnable runnable);
}
