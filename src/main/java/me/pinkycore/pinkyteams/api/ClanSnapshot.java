package me.pinkycore.pinkyteams.api;

import java.util.List;

public record ClanSnapshot(String name, String tag, String leader, String founder,
                           String privacy, double balance, int points, List<String> members) {
    public ClanSnapshot {
        members = List.copyOf(members);
    }
}
