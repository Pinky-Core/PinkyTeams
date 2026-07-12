package me.pinkycore.pinkyteams.api;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PinkyTeamsAPI {
    Optional<String> getPlayerClan(UUID playerId);
    Optional<String> getPlayerClan(String playerName);
    Optional<ClanSnapshot> getClan(String clanName);
    Set<String> getClanNames();
    boolean areAllied(String firstClan, String secondClan);
}
