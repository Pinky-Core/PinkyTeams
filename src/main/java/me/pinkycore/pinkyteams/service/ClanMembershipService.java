package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.Database.StorageProvider;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class ClanMembershipService {
    private final StorageProvider storage;
    private final Random random;

    public ClanMembershipService(StorageProvider storage) {
        this(storage, new Random());
    }

    ClanMembershipService(StorageProvider storage, Random random) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.random = Objects.requireNonNull(random, "random");
    }

    public void join(String playerName, String clanName) {
        storage.addPlayerToClan(playerName, clanName);
        storage.removeClanInvite(clanName, playerName);
        storage.reloadCache();
    }

    public LeaveResult leave(String playerName, String clanName) {
        String leader = storage.getClanLeader(clanName);
        boolean wasLeader = leader != null && leader.equalsIgnoreCase(playerName);
        storage.removePlayerFromClan(playerName, clanName);

        List<String> remaining = storage.getClanMembers(clanName);
        if (remaining.isEmpty()) {
            storage.deleteClan(clanName);
            storage.reloadCache();
            return LeaveResult.deletedClan();
        }

        String newLeader = null;
        if (wasLeader) {
            newLeader = remaining.get(random.nextInt(remaining.size()));
            storage.updateClanLeader(clanName, newLeader);
        }
        storage.reloadCache();
        return new LeaveResult(false, wasLeader, newLeader);
    }

    public record LeaveResult(boolean clanDeleted, boolean leaderChanged, String newLeader) {
        static LeaveResult deletedClan() { return new LeaveResult(true, false, null); }
    }
}
