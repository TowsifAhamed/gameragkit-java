package org.bukkit.plugin.java;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public abstract class JavaPlugin implements Plugin {
    public void onEnable() {
        throw new UnsupportedOperationException("Stub");
    }

    public void onDisable() {
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public void saveDefaultConfig() {
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public FileConfiguration getConfig() {
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public Server getServer() {
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public Logger getLogger() {
        throw new UnsupportedOperationException("Stub");
    }
}
