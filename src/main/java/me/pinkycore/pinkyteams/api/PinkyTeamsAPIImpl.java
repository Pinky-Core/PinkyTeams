package me.pinkycore.pinkyteams.api;

import me.pinkycore.pinkyteams.Database.StorageProvider;
import org.bukkit.Bukkit;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PinkyTeamsAPIImpl implements PinkyTeamsAPI {
    private final StorageProvider storage;

    public PinkyTeamsAPIImpl(StorageProvider storage) {
        this.storage = storage;
    }

    public Optional<String> getPlayerClan(UUID playerId) {
        if (playerId == null) return Optional.empty();
        String name = Bukkit.getOfflinePlayer(playerId).getName();
        return getPlayerClan(name);
    }

    public Optional<String> getPlayerClan(String playerName) {
        if (playerName == null || playerName.isBlank()) return Optional.empty();
        return Optional.ofNullable(storage.getPlayerClan(playerName));
    }

    public Optional<ClanSnapshot> getClan(String clanName) {
        if (clanName == null || clanName.isBlank() || !storage.clanExists(clanName)) return Optional.empty();
        return Optional.of(new ClanSnapshot(clanName, storage.getClanColoredName(clanName),
            storage.getClanLeader(clanName), storage.getClanFounder(clanName),
            storage.getClanPrivacy(clanName), storage.getClanMoney(clanName),
            storage.getClanPoints(clanName), storage.getClanMembers(clanName)));
    }

    public Set<String> getClanNames() {
        return Set.copyOf(storage.getCachedClanNames());
    }

    public boolean areAllied(String firstClan, String secondClan) {
        return firstClan != null && secondClan != null && storage.areClansAllied(firstClan, secondClan);
    }
}
