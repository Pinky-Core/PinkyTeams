package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.PinkyTeams;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class PlayerIdentityService {
    private final PinkyTeams plugin;
    private final File file;
    private final YamlConfiguration identities;

    public PlayerIdentityService(PinkyTeams plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "identities.yml");
        this.identities = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void observe(UUID uuid, String currentName) {
        if (uuid == null || currentName == null || currentName.isBlank()) return;
        String path = "players." + uuid;
        String previousName = identities.getString(path + ".name");
        if (previousName != null && !previousName.equalsIgnoreCase(currentName)) {
            plugin.getLogger().info("Migrating player identity " + previousName + " -> " + currentName + " (" + uuid + ")");
            plugin.getStorageProvider().renamePlayerIdentity(previousName, currentName);
        }
        identities.set(path + ".name", currentName);
        identities.set(path + ".last-seen", System.currentTimeMillis());
        save();
    }

    public String getLastKnownName(UUID uuid) {
        return uuid == null ? null : identities.getString("players." + uuid + ".name");
    }

    private void save() {
        try {
            identities.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save identities.yml: " + e.getMessage());
        }
    }
}
