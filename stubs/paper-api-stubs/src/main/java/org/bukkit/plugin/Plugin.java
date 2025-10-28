package org.bukkit.plugin;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

public interface Plugin {
    void saveDefaultConfig();

    FileConfiguration getConfig();

    Server getServer();

    Logger getLogger();
}
