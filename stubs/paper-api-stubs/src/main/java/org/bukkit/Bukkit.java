package org.bukkit;

import org.bukkit.scheduler.BukkitScheduler;

/**
 * Minimal stub of the Bukkit entry point for compilation of the sample plugin.
 */
public final class Bukkit {
    private Bukkit() {
    }

    public static BukkitScheduler getScheduler() {
        throw new UnsupportedOperationException("Stub");
    }

    public static Server getServer() {
        throw new UnsupportedOperationException("Stub");
    }
}
