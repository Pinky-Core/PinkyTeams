package me.pinkycore.pinkyteams.Utils;

import me.pinkycore.pinkyteams.PinkyTeams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyDataMigratorTest {
    @TempDir Path pluginsDirectory;

    @Test
    void copiesLegacyDataWithoutDeletingSource() throws Exception {
        Path legacy = Files.createDirectories(pluginsDirectory.resolve("VanguardClans"));
        Files.writeString(legacy.resolve("clans.db"), "legacy-data");
        Path target = pluginsDirectory.resolve("PinkyTeams");
        PinkyTeams plugin = mock(PinkyTeams.class);
        when(plugin.getDataFolder()).thenReturn(target.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        LegacyDataMigrator.migrateIfNeeded(plugin);

        assertEquals("legacy-data", Files.readString(target.resolve("clans.db")));
        assertTrue(Files.exists(target.resolve(".vanguardclans-migrated")));
        assertTrue(LegacyDataMigrator.isMigrationComplete(plugin));
        assertTrue(Files.exists(legacy.resolve("clans.db")));
    }

    @Test
    void neverOverwritesExistingPinkyTeamsData() throws Exception {
        Path legacy = Files.createDirectories(pluginsDirectory.resolve("VanguardClans"));
        Files.writeString(legacy.resolve("config.yml"), "legacy");
        Path target = Files.createDirectories(pluginsDirectory.resolve("PinkyTeams"));
        Files.writeString(target.resolve("config.yml"), "current");
        PinkyTeams plugin = mock(PinkyTeams.class);
        when(plugin.getDataFolder()).thenReturn(target.toFile());

        LegacyDataMigrator.migrateIfNeeded(plugin);

        assertEquals("current", Files.readString(target.resolve("config.yml")));
        assertFalse(Files.exists(target.resolve(".vanguardclans-migrated")));
        assertFalse(LegacyDataMigrator.isMigrationComplete(plugin));
    }
}
