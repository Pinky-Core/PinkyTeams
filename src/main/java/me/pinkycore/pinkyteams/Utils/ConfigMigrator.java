package me.pinkycore.pinkyteams.Utils;

import me.pinkycore.pinkyteams.PinkyTeams;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ConfigMigrator {
    public static final int CURRENT_VERSION = 5;

    private ConfigMigrator() {
    }

    public static void migrate(PinkyTeams plugin) {
        FileConfiguration config = plugin.getConfig();
        int previous = config.getInt("config-version", 0);
        if (previous >= CURRENT_VERSION) return;

        backup(plugin, previous);
        config.options().copyDefaults(true);
        config.set("config-version", CURRENT_VERSION);
        plugin.saveConfig();
        plugin.reloadConfig();
        plugin.getLogger().info("Configuration migrated from version " + previous + " to " + CURRENT_VERSION + ".");
    }

    private static void backup(PinkyTeams plugin, int previous) {
        File source = new File(plugin.getDataFolder(), "config.yml");
        if (!source.isFile()) return;
        File backup = new File(plugin.getDataFolder(), "config.v" + previous + ".backup.yml");
        if (backup.exists()) return;
        try {
            Files.copy(source.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not back up config.yml before migration: " + e.getMessage());
        }
    }
}
