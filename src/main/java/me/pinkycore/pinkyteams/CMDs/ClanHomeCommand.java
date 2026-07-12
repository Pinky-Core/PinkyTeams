package me.pinkycore.pinkyteams.CMDs;

import me.pinkycore.pinkyteams.PinkyTeams;
import me.pinkycore.pinkyteams.Utils.LangManager;
import me.pinkycore.pinkyteams.Utils.MSG;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;

final class ClanHomeCommand {
    private final PinkyTeams plugin;
    private final LangManager lang;

    ClanHomeCommand(PinkyTeams plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    void set(Player player, String clan) {
        Location location = player.getLocation();
        if (!isSafeLocation(location)) {
            send(player, "user.home_unsafe_set");
            return;
        }
        plugin.getStorageProvider().setClanHome(clan, location);
        send(player, "user.home_set");
    }

    void delete(Player player, String clan) {
        plugin.getStorageProvider().deleteClanHome(clan);
        send(player, "user.home_deleted");
    }

    void teleport(Player player, String clan) {
        UUID uuid = player.getUniqueId();
        if (plugin.teleportingPlayers.contains(uuid)) {
            send(player, "user.on_teport");
            return;
        }

        boolean bypassCooldown = player.hasPermission("pinkyteams.bypass.homecooldown");
        boolean bypassDelay = player.hasPermission("pinkyteams.bypass.homedelay");
        if (!bypassCooldown) {
            long lastUsed = plugin.homeCooldowns.getOrDefault(uuid, 0L);
            long timeLeft = ((lastUsed + plugin.clanHomeCooldown * 1000L) - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                player.sendMessage(MSG.color(lang.getMessageWithPrefix("user.home_cooldown")
                    .replace("{seconds}", String.valueOf(timeLeft))));
                return;
            }
            plugin.homeCooldowns.put(uuid, System.currentTimeMillis());
        }

        if (bypassDelay) {
            finishTeleport(player, clan);
            return;
        }

        plugin.teleportingPlayers.add(uuid);
        player.sendMessage(MSG.color(lang.getMessageWithPrefix("user.teleporting_home")
            .replace("{seconds}", String.valueOf(plugin.clanHomeDelay))));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!plugin.teleportingPlayers.remove(uuid)) return;
            finishTeleport(player, clan);
        }, plugin.clanHomeDelay * 20L);
    }

    private void finishTeleport(Player player, String clan) {
        Location home = plugin.getStorageProvider().getClanHome(clan);
        if (home == null) {
            send(player, "user.home_not_set");
        } else if (!canTeleport(home)) {
            send(player, "user.home_unsafe_teleport");
        } else {
            player.teleport(home);
            send(player, "user.home_teleported");
        }
    }

    private boolean canTeleport(Location location) {
        FileConfiguration config = plugin.getFH().getConfig();
        return !config.getBoolean("clan_home.safe-location-protection.enabled", true)
            || !config.getBoolean("clan_home.safe-location-protection.check-before-teleport", true)
            || isSafeLocation(location);
    }

    private boolean isSafeLocation(Location location) {
        FileConfiguration config = plugin.getFH().getConfig();
        if (!config.getBoolean("clan_home.safe-location-protection.enabled", true)) return true;
        if (location == null || location.getWorld() == null) return false;
        World world = location.getWorld();
        if (location.getY() < world.getMinHeight() || location.getY() >= world.getMaxHeight() - 1
            || !world.getWorldBorder().isInside(location)) return false;

        Material feet = location.getBlock().getType();
        Material head = location.clone().add(0, 1, 0).getBlock().getType();
        Material ground = location.clone().subtract(0, 1, 0).getBlock().getType();
        if (config.getBoolean("clan_home.safe-location-protection.require-feet-and-head-space", true)
            && (!isSafeSpace(feet) || !isSafeSpace(head))) return false;
        if (config.getBoolean("clan_home.safe-location-protection.require-solid-ground", true)
            && (!ground.isSolid() || isAir(ground))) return false;
        return !config.getBoolean("clan_home.safe-location-protection.block-dangerous-materials", true)
            || (!isDangerous(feet) && !isDangerous(head) && !isDangerous(ground));
    }

    static boolean isDangerous(Material material) {
        if (material == null) return true;
        String name = material.name();
        return name.contains("LAVA") || name.contains("FIRE") || name.contains("MAGMA")
            || name.contains("CACTUS") || name.contains("SWEET_BERRY_BUSH")
            || name.contains("POWDER_SNOW") || name.contains("CAMPFIRE") || name.contains("WITHER_ROSE");
    }

    private static boolean isSafeSpace(Material material) { return isAir(material) || !material.isSolid(); }
    private static boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }
    private void send(Player player, String key) { player.sendMessage(MSG.color(lang.getMessageWithPrefix(key))); }
}
