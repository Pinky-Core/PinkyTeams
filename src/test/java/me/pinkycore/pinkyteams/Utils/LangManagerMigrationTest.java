package me.pinkycore.pinkyteams.Utils;

import me.pinkycore.pinkyteams.PinkyTeams;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LangManagerMigrationTest {
    @TempDir Path directory;

    @Test void mergesNewKeysPreservesCustomizationAndMigratesBranding() throws Exception {
        Path lang = Files.createDirectories(directory.resolve("lang"));
        Files.writeString(lang.resolve("es.yml"), """
            user:
              help_title: "&dVanguardClans Ayuda"
              no_permission: "&cMi mensaje personalizado"
            """);
        PinkyTeams plugin = mock(PinkyTeams.class);
        YamlConfiguration mainConfig = new YamlConfiguration();
        mainConfig.set("lang", "es");
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getConfig()).thenReturn(mainConfig);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("lang-migration-test"));
        when(plugin.getResource("lang/es.yml")).thenAnswer(invocation -> resource("lang/es.yml"));

        LangManager manager = new LangManager(plugin);

        assertEquals("&dPinkyTeams Ayuda", manager.getMessage("user.help_title"));
        assertEquals("&cMi mensaje personalizado", manager.getMessage("user.no_permission"));
        assertFalse(manager.getMessage("gui.item_refresh").contains("Mensaje no encontrado"));
        assertTrue(YamlConfiguration.loadConfiguration(lang.resolve("es.yml").toFile()).contains("gui.item_refresh"));
    }

    private InputStream resource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}
