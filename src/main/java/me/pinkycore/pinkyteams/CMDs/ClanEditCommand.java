package me.pinkycore.pinkyteams.CMDs;

import me.pinkycore.pinkyteams.PinkyTeams;
import me.pinkycore.pinkyteams.Utils.ClanNameHandler;
import me.pinkycore.pinkyteams.Utils.ClanPermission;
import me.pinkycore.pinkyteams.Utils.Econo;
import me.pinkycore.pinkyteams.Utils.LangManager;
import me.pinkycore.pinkyteams.Utils.MSG;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

final class ClanEditCommand {
    private final PinkyTeams plugin;
    private final LangManager lang;
    private final ClanEconomyCommand.PermissionCheck permission;

    ClanEditCommand(PinkyTeams plugin, LangManager lang, ClanEconomyCommand.PermissionCheck permission) {
        this.plugin = plugin;
        this.lang = lang;
        this.permission = permission;
    }

    void execute(Player player, String clan, String[] args) {
        if (args.length < 3) { send(player, "user.edit_usage"); return; }
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "name" -> rename(player, clan, value);
            case "tag" -> tag(player, clan, value);
            case "privacy" -> privacy(player, clan, value);
            default -> send(player, "user.edit_usage");
        }
    }

    private void rename(Player player, String oldName, String rawName) {
        if (!permission.test(player, oldName, ClanPermission.EDIT_NAME)) return;
        String newName = ClanNameHandler.getVisibleName(rawName);
        FileConfiguration config = plugin.getConfig();
        int min = config.getInt("clan-name.min-length", 0);
        int max = config.getInt("clan-name.max-length", 16);
        if (max > 0 && newName.length() > max) { message(player, "user.edit_name_too_long", "{max}", max); return; }
        if (min > 0 && newName.length() < min) { message(player, "user.edit_name_too_short", "{min}", min); return; }
        if (plugin.isClanBanned(newName)) { message(player, "msg.clan_name_banned", "{clan}", newName); return; }
        if (config.getStringList("names-blocked.blocked").stream().anyMatch(v -> v.equalsIgnoreCase(newName))) {
            send(player, "user.edit_name_blocked"); return;
        }
        if (!validRegex(config, "clan-name.regex", newName)) { send(player, "user.edit_name_invalid_regex"); return; }
        boolean same = oldName.equalsIgnoreCase(newName);
        if (!same && plugin.getStorageProvider().clanExists(newName)) { send(player, "user.edit_name_exists"); return; }
        long cooldown = cooldownLeft(oldName);
        if (!same && cooldown > 0) {
            message(player, "user.edit_name_cooldown", "{time}", duration(cooldown), "{seconds}", cooldown); return;
        }
        double cost = renameCost(config, same);
        Econo economy = PinkyTeams.getEcon();
        if (cost > 0 && (!economy.has(player, cost) || !economy.withdraw(player, cost))) {
            message(player, "user.edit_name_no_money", "{cost}", new DecimalFormat("#,##0.##").format(cost)); return;
        }
        persistRename(player, oldName, newName, rawName, same, cost, economy);
    }

    private void persistRename(Player player, String oldName, String newName, String rawName,
                               boolean same, double cost, Econo economy) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getStorageProvider().updateClanName(oldName, newName, rawName);
                plugin.getStorageProvider().reloadCache();
                if (!newName.equalsIgnoreCase(plugin.getStorageProvider().getPlayerClan(player.getName())))
                    throw new IllegalStateException("Rename was not persisted");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.notifyClanRenamed(oldName, newName);
                    if (!same) setCooldown(newName);
                    plugin.refreshClanVisuals();
                    message(player, "user.edit_name_success", "{name}", MSG.color(rawName));
                });
            } catch (Exception failure) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (cost > 0 && !economy.deposit(player, cost))
                        plugin.getLogger().severe("Failed rename refund for " + player.getName());
                    send(player, "user.edit_name_error");
                });
            }
        });
    }

    private void tag(Player player, String clan, String rawTag) {
        if (!permission.test(player, clan, ClanPermission.EDIT_TAG)) return;
        String visible = ClanNameHandler.getVisibleName(rawTag);
        int max = plugin.getConfig().getInt("clan-name.max-length", 16);
        if (max > 0 && visible.length() > max) { message(player, "user.edit_name_too_long", "{max}", max); return; }
        if (!validRegex(plugin.getConfig(), "clan-tag.regex", visible)) { send(player, "user.edit_tag_invalid_regex"); return; }
        String colored = MSG.color(rawTag);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getStorageProvider().setClanColoredName(clan, colored);
            plugin.getStorageProvider().reloadCache();
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.refreshClanVisuals();
                message(player, "user.edit_tag_success", "{name}", colored);
            });
        });
    }

    private void privacy(Player player, String clan, String raw) {
        if (!permission.test(player, clan, ClanPermission.EDIT_PRIVACY)) return;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (!value.equals("public") && !value.equals("private")) { send(player, "user.edit_privacy_usage"); return; }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getStorageProvider().setClanPrivacy(clan, value);
            plugin.getStorageProvider().reloadCache();
            Bukkit.getScheduler().runTask(plugin,
                () -> message(player, "user.edit_privacy_success", "{privacy}", value));
        });
    }

    private double renameCost(FileConfiguration config, boolean same) {
        return !same && config.getBoolean("clan-name.rename.fee.enabled", false)
            && config.getBoolean("economy.enabled", true)
            ? Math.max(0, config.getDouble("clan-name.rename.fee.amount", 0)) : 0;
    }

    private boolean validRegex(FileConfiguration config, String path, String value) {
        if (!config.getBoolean(path + ".enabled", false)) return true;
        try { return Pattern.matches(config.getString(path + ".pattern", "^[A-Za-z0-9_]+$"), value); }
        catch (Exception invalid) { plugin.getLogger().warning("Invalid regex at " + path); return false; }
    }

    private long cooldownLeft(String clan) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("clan-name.rename.cooldown.enabled", false)) return 0;
        long configured = config.getLong("clan-name.rename.cooldown.seconds", 0);
        long last = plugin.getFH().getData().getLong("clan-name-renames." + clan.toLowerCase(Locale.ROOT), 0);
        return Math.max(0, configured - (System.currentTimeMillis() - last) / 1000L);
    }

    private void setCooldown(String clan) {
        plugin.getFH().getData().set("clan-name-renames." + clan.toLowerCase(Locale.ROOT), System.currentTimeMillis());
        plugin.getFH().saveData();
    }

    private String duration(long seconds) {
        long days = seconds / 86400, hours = seconds % 86400 / 3600;
        long minutes = seconds % 3600 / 60, rest = seconds % 60;
        if (days > 0) return days + unit("days", "d") + " " + hours + unit("hours", "h");
        if (hours > 0) return hours + unit("hours", "h") + " " + minutes + unit("minutes", "m");
        return minutes > 0 ? minutes + unit("minutes", "m") + " " + rest + unit("seconds", "s")
            : rest + unit("seconds", "s");
    }

    private String unit(String key, String fallback) { return plugin.getConfig().getString("time-format." + key, fallback); }
    private void send(Player player, String key) { player.sendMessage(MSG.color(lang.getMessageWithPrefix(key))); }
    private void message(Player player, String key, Object... values) {
        String text = lang.getMessageWithPrefix(key);
        for (int i = 0; i + 1 < values.length; i += 2) text = text.replace(String.valueOf(values[i]), String.valueOf(values[i + 1]));
        player.sendMessage(MSG.color(text));
    }
}
