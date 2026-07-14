package me.pinkycore.pinkyteams.Utils;

import me.pinkycore.pinkyteams.PinkyTeams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/** Copies a legacy VanguardClans data directory into PinkyTeams once, without overwriting files. */
public final class LegacyDataMigrator {
    private static final String LEGACY_FOLDER = "VanguardClans";

    private LegacyDataMigrator() {
    }

    public static void migrateIfNeeded(PinkyTeams plugin) {
        Path target = plugin.getDataFolder().toPath();
        Path pluginsDirectory = target.getParent();
        if (pluginsDirectory == null) return;

        Path legacy = pluginsDirectory.resolve(LEGACY_FOLDER);
        Path marker = target.resolve(".vanguardclans-migrated");
        if (!Files.isDirectory(legacy) || Files.exists(marker) || hasUserData(target)) return;

        plugin.getLogger().info("Legacy VanguardClans data found. Starting one-time migration...");
        try {
            Files.createDirectories(target);
            try (Stream<Path> paths = Files.walk(legacy)) {
                paths.forEach(source -> copyMissing(source, legacy, target, plugin));
            }
            Files.writeString(marker,
                "Migrated from " + legacy.toAbsolutePath() + System.lineSeparator());
            plugin.getLogger().info("Legacy data migration completed. The original directory was preserved.");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not migrate legacy data: " + e.getMessage());
        }
    }

    public static boolean isMigrationComplete(PinkyTeams plugin) {
        return Files.isRegularFile(plugin.getDataFolder().toPath().resolve(".vanguardclans-migrated"));
    }

    private static boolean hasUserData(Path target) {
        return Files.exists(target.resolve("config.yml"))
            || Files.exists(target.resolve("data.yml"))
            || Files.exists(target.resolve("clans.db"))
            || Files.exists(target.resolve("clans.mv.db"));
    }

    private static void copyMissing(Path source, Path legacy, Path target, PinkyTeams plugin) {
        Path destination = target.resolve(legacy.relativize(source));
        try {
            if (Files.isDirectory(source)) {
                Files.createDirectories(destination);
            } else if (!Files.exists(destination)) {
                Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not migrate " + source.getFileName() + ": " + e.getMessage());
        }
    }
}
