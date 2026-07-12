package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.Database.StorageProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ClanAllianceServiceTest {
    @Test
    void requestCreatesPendingAlliance() {
        StorageProvider storage = mock(StorageProvider.class);
        when(storage.clanExists("Target")).thenReturn(true);
        when(storage.getPendingAlliances("Target")).thenReturn(List.of());
        ClanAllianceService service = new ClanAllianceService(storage);

        assertEquals(ClanAllianceService.Result.SUCCESS, service.request("Source", "Target"));
        verify(storage).addPendingAlliance("Source", "Target");
    }

    @Test
    void duplicateRequestIsRejectedCaseInsensitively() {
        StorageProvider storage = mock(StorageProvider.class);
        when(storage.clanExists("Target")).thenReturn(true);
        when(storage.getPendingAlliances("Target")).thenReturn(List.of("SOURCE"));
        ClanAllianceService service = new ClanAllianceService(storage);

        assertEquals(ClanAllianceService.Result.ALREADY_PENDING, service.request("Source", "Target"));
        verify(storage, never()).addPendingAlliance(anyString(), anyString());
    }

    @Test
    void acceptingCreatesAllianceAndConsumesRequest() {
        StorageProvider storage = mock(StorageProvider.class);
        when(storage.getPendingAlliances("Target")).thenReturn(List.of("Source"));
        ClanAllianceService service = new ClanAllianceService(storage);

        assertEquals(ClanAllianceService.Result.SUCCESS, service.accept("Target", "Source"));
        verify(storage).createAlliance("Source", "Target", false);
        verify(storage).removePendingAlliance("Source", "Target");
        verify(storage).reloadCache();
    }

    @Test
    void removingNonAllianceDoesNothing() {
        StorageProvider storage = mock(StorageProvider.class);
        when(storage.getClanAlliances("Source")).thenReturn(List.of());
        ClanAllianceService service = new ClanAllianceService(storage);

        assertEquals(ClanAllianceService.Result.NOT_ALLIED, service.remove("Source", "Target"));
        verify(storage, never()).removeAlliance(anyString(), anyString());
    }
}
