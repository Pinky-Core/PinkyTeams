package me.pinkycore.pinkyteams.Utils;

import me.pinkycore.pinkyteams.PinkyTeams;

import java.sql.*;

import org.bukkit.World;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

public class ClanUtils {

    private static PinkyTeams plugin;

    public static void init(PinkyTeams instance) {
        plugin = instance;
    }

    public static boolean isFriendlyFireEnabledAllies(String clan) {
        return PinkyTeams.getInstance().getStorageProvider().isFriendlyFireAlliesEnabled(clan);
    }

    public static boolean areClansAllied(String clan1, String clan2) {
        return PinkyTeams.getInstance().getStorageProvider().areClansAllied(clan1, clan2);
    }

    public boolean isWorldBlocked(World world) {
        List<String> blocked = plugin.getConfig().getStringList("blocked-worlds");
        return blocked.contains(world.getName());
    }
}
