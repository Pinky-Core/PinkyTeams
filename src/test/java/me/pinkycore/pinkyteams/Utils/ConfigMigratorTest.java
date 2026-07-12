package me.pinkycore.pinkyteams.Utils;
import me.pinkycore.pinkyteams.PinkyTeams;import org.bukkit.configuration.file.YamlConfiguration;import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;import java.util.logging.Logger;import static org.junit.jupiter.api.Assertions.*;import static org.mockito.Mockito.*;
class ConfigMigratorTest{
 @TempDir Path dir;
 @Test void upgradesVersionAndCreatesBackup() throws Exception{Path source=dir.resolve("config.yml");Files.writeString(source,"config-version: 2\ncustom-value: keep\n");
  PinkyTeams plugin=mock(PinkyTeams.class);YamlConfiguration config=YamlConfiguration.loadConfiguration(source.toFile());when(plugin.getDataFolder()).thenReturn(dir.toFile());when(plugin.getConfig()).thenReturn(config);when(plugin.getLogger()).thenReturn(Logger.getLogger("config-test"));
  ConfigMigrator.migrate(plugin);assertEquals(ConfigMigrator.CURRENT_VERSION,config.getInt("config-version"));assertEquals("keep",config.getString("custom-value"));assertTrue(Files.exists(dir.resolve("config.v2.backup.yml")));verify(plugin).saveConfig();verify(plugin).reloadConfig();}
}
