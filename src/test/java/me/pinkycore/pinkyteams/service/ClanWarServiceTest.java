package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.Database.StorageProvider;
import me.pinkycore.pinkyteams.PinkyTeams;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClanWarServiceTest {
    @TempDir Path directory;

    @Test
    void requestAcceptAndScoreWar() {
        PinkyTeams plugin = plugin(2);
        ClanWarService service = new ClanWarService(plugin);
        assertEquals(ClanWarService.Result.SUCCESS, service.request("Alpha", "Beta"));
        assertEquals(ClanWarService.Result.SUCCESS, service.accept("Beta", "Alpha"));
        assertTrue(service.findWar("Alpha").isPresent());

        assertTrue(service.recordKill("Alpha", "Beta", UUID.randomUUID(), UUID.randomUUID(), "1").isEmpty());
        var finished = service.recordKill("Alpha", "Beta", UUID.randomUUID(), UUID.randomUUID(), "2");
        assertTrue(finished.isPresent());
        assertEquals("Alpha", finished.get().winner());
        assertTrue(service.findWar("Alpha").isEmpty());
    }

    @Test
    void antiFarmRejectsRepeatedKill() {
        PinkyTeams plugin = plugin(20);
        ClanWarService service = new ClanWarService(plugin);
        service.request("Alpha", "Beta"); service.accept("Beta", "Alpha");
        UUID killer=UUID.randomUUID(), victim=UUID.randomUUID();
        service.recordKill("Alpha","Beta",killer,victim,"same-ip");
        service.recordKill("Alpha","Beta",killer,victim,"same-ip");
        assertEquals(1, service.findWar("Alpha").orElseThrow().scoreFor("Alpha"));
    }

    private PinkyTeams plugin(int scoreToWin) {
        PinkyTeams plugin=mock(PinkyTeams.class); StorageProvider storage=mock(StorageProvider.class);
        YamlConfiguration config=new YamlConfiguration();
        config.set("wars.request-expiration-seconds",300); config.set("wars.duration-seconds",3600);
        config.set("wars.score-to-win",scoreToWin); config.set("wars.anti-farm.cooldown-seconds",300);
        when(plugin.getDataFolder()).thenReturn(directory.toFile()); when(plugin.getConfig()).thenReturn(config);
        when(plugin.getStorageProvider()).thenReturn(storage); when(storage.clanExists("Beta")).thenReturn(true);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("war-test")); return plugin;
    }
}
