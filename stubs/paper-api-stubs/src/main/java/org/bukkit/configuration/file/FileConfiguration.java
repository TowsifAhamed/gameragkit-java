package org.bukkit.configuration.file;

public interface FileConfiguration {
    String getString(String path);

    String getString(String path, String def);
}
