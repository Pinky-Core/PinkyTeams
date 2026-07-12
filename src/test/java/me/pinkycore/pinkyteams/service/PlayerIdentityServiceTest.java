package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.Database.StorageProvider;
import me.pinkycore.pinkyteams.PinkyTeams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PlayerIdentityServiceTest {
    @TempDir Path directory;

    @Test
    void sameUuidWithNewNameMigratesStorage() {
        PinkyTeams plugin = mock(PinkyTeams.class);
        StorageProvider storage = mock(StorageProvider.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getStorageProvider()).thenReturn(storage);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("identity-test"));
        PlayerIdentityService service = new PlayerIdentityService(plugin);
        UUID uuid = UUID.randomUUID();

        service.observe(uuid, "OldName");
        service.observe(uuid, "NewName");

        verify(storage).renamePlayerIdentity("OldName", "NewName");
        assertEquals("NewName", service.getLastKnownName(uuid));
    }

    @Test
    void unchangedNameDoesNotTouchStorage() {
        PinkyTeams plugin = mock(PinkyTeams.class);
        StorageProvider storage = mock(StorageProvider.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getStorageProvider()).thenReturn(storage);
        PlayerIdentityService service = new PlayerIdentityService(plugin);
        UUID uuid = UUID.randomUUID();

        service.observe(uuid, "Pinky");
        service.observe(uuid, "pinky");

        verify(storage, never()).renamePlayerIdentity(anyString(), anyString());
    }
}
