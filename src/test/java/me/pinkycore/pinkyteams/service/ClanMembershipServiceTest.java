package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.Database.StorageProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClanMembershipServiceTest {
    @Test
    void joinAddsPlayerConsumesInviteAndRefreshesCache() {
        StorageProvider storage = mock(StorageProvider.class);
        ClanMembershipService service = new ClanMembershipService(storage);

        service.join("Pinky", "Team");

        verify(storage).addPlayerToClan("Pinky", "Team");
        verify(storage).removeClanInvite("Team", "Pinky");
        verify(storage).reloadCache();
    }

    @Test
    void lastMemberLeavingDeletesClan() {
        StorageProvider storage = mock(StorageProvider.class);
        when(storage.getClanLeader("Team")).thenReturn("Pinky");
        when(storage.getClanMembers("Team")).thenReturn(List.of());
        ClanMembershipService service = new ClanMembershipService(storage);

        ClanMembershipService.LeaveResult result = service.leave("Pinky", "Team");

        assertTrue(result.clanDeleted());
        verify(storage).deleteClan("Team");
        verify(storage).reloadCache();
    }

    @Test
    void leaderLeavingTransfersLeadership() {
        StorageProvider storage = mock(StorageProvider.class);
        when(storage.getClanLeader("Team")).thenReturn("Pinky");
        when(storage.getClanMembers("Team")).thenReturn(List.of("NewLeader"));
        ClanMembershipService service = new ClanMembershipService(storage, new Random(1));

        ClanMembershipService.LeaveResult result = service.leave("Pinky", "Team");

        assertTrue(result.leaderChanged());
        assertEquals("NewLeader", result.newLeader());
        verify(storage).updateClanLeader("Team", "NewLeader");
    }
}
