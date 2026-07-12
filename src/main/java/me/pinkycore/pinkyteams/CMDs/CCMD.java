package me.pinkycore.pinkyteams.CMDs;

import me.pinkycore.pinkyteams.PinkyTeams;
import me.pinkycore.pinkyteams.Utils.Econo;
import me.pinkycore.pinkyteams.Utils.FileHandler;
import me.pinkycore.pinkyteams.Utils.MSG;
import static me.pinkycore.pinkyteams.PinkyTeams.prefix;
import me.pinkycore.pinkyteams.Utils.LangManager;
import me.pinkycore.pinkyteams.Utils.ClanNameHandler;
import me.pinkycore.pinkyteams.Utils.ClanPermission;
import me.pinkycore.pinkyteams.Utils.ClanRoleManager;
import me.pinkycore.pinkyteams.Utils.TopMetric;
import me.pinkycore.pinkyteams.Database.StorageProvider;
import me.pinkycore.pinkyteams.api.event.*;
import me.pinkycore.pinkyteams.service.ClanMembershipService;
import me.pinkycore.pinkyteams.service.ClanAllianceService;


import java.util.*;
import java.util.stream.Collectors;
import java.text.DecimalFormat;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.ChatColor;



import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class CCMD implements CommandExecutor, TabCompleter, Listener {
    private final PinkyTeams plugin;
    private final LangManager langManager;
    private List<String> helpLines;
    private final ClanEconomyCommand economyCommand;
    private final ClanHomeCommand homeCommand;
    private final ClanWarCommand warCommand;
    private final ClanRoleCommand roleCommand;
    private final ClanSlotCommand slotCommand;
    private final ClanInvitationCommand invitationCommand;
    private final ClanAllianceCommand allianceCommand;
    private final ClanReportCommand reportCommand;
    private final ClanLifecycleCommand lifecycleCommand;
    private final ClanQueryCommand queryCommand;
    private final ClanEditCommand editCommand;
    public Set<UUID> teleportingPlayers = new HashSet<>();
    private final Map<UUID, Long> homeCooldowns = new HashMap<>();
    
    

    public CCMD(PinkyTeams plugin, LangManager langManager) {
        this.plugin = plugin;
        this.langManager = langManager;
        this.helpLines = langManager.getMessageList("user.help_lines");
        this.economyCommand = new ClanEconomyCommand(plugin, langManager, this::hasClanPermission);
        this.homeCommand = new ClanHomeCommand(plugin, langManager);
        this.warCommand = new ClanWarCommand(plugin, langManager);
        this.roleCommand = new ClanRoleCommand(plugin,langManager,this::hasClanPermission);
        this.slotCommand = new ClanSlotCommand(plugin,langManager,this::hasClanPermission);
        this.invitationCommand = new ClanInvitationCommand(plugin,langManager,this::hasClanPermission);
        this.allianceCommand = new ClanAllianceCommand(plugin,langManager,this::hasClanPermission);
        this.reportCommand = new ClanReportCommand(plugin,langManager);
        this.lifecycleCommand = new ClanLifecycleCommand(plugin,langManager,this::hasClanPermission);
        this.queryCommand = new ClanQueryCommand(plugin,langManager);
        this.editCommand = new ClanEditCommand(plugin,langManager,this::hasClanPermission);
    }

    public void reloadHelpLines() {
        this.helpLines = langManager.getMessageList("user.help_lines");
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MSG.color(langManager.getMessage("user.console_command_only")));
            return true;
        }

        if (plugin.isWorldBlocked(player.getWorld())) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.command_blocked_world")));
            return true;
        }

        String playerName = player.getName();
        String playerClan = this.getPlayerClan(playerName);
        boolean showMainHelpPage = shouldShowMainHelpPage();

        // Comando de ayuda paginada
        if (args.length < 1) {
            if (plugin.getGuiManager() != null) {
                plugin.getGuiManager().openMainMenu(player);
                return true;
            }
            if (!showMainHelpPage) {
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.command_not_found")));
                return true;
            }
            help(player, 1);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            int page = 1;
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.invalid_page_number")));
                    return true;
                }
            }

            // Reutiliza la ayuda paginada con sus botones de navegación.
            help(player, page);
            return true;
        }
        // Resto de comandos con permisos individuales
        switch (args[0].toLowerCase()) {
            case "create":
                if (!player.hasPermission("pinkyteams.user.create")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan != null && !playerClan.isEmpty()) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.already_in_clan")));
                    return true;
                }
                lifecycleCommand.create(player,args);
                break;

            case "disband":
                if (!player.hasPermission("pinkyteams.user.disband")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                lifecycleCommand.disband(player,playerClan,args);
                break;

            case "sethome":
                if (!player.hasPermission("pinkyteams.user.sethome")) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }
                if (!hasClanPermission(player, playerClan, ClanPermission.SETHOME)) {
                    return true;
                }
                homeCommand.set(player, playerClan);
                break;

            case "delhome":
                if (!player.hasPermission("pinkyteams.user.delhome")) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }
                if (!hasClanPermission(player, playerClan, ClanPermission.DELHOME)) {
                    return true;
                }
                homeCommand.delete(player, playerClan);
                break;

            case "home":
                if (!player.hasPermission("pinkyteams.user.home")) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }
                homeCommand.teleport(player, playerClan);
                break;

            case "report":
                if (!player.hasPermission("pinkyteams.user.report")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_report")));
                    return true;
                }
                reportCommand.execute(sender,args[1],String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                break;

            case "menu":
            case "gui":
                if (plugin.getGuiManager() != null) {
                    plugin.getGuiManager().openMainMenu(player);
                    break;
                }
                sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.command_not_found")));
                break;

            case "commands":
                help(player, 1);
                break;

            case "top":
                if (!player.hasPermission("pinkyteams.user.top")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                handleTopCommand(player, args);
                break;

            case "rank":
                if (!player.hasPermission("pinkyteams.user.rank")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                roleCommand.execute(player, playerClan, args);
                break;

            case "list":
                if (!player.hasPermission("pinkyteams.user.list")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                queryCommand.list(sender);
                break;

            case "war":
                if (!player.hasPermission("pinkyteams.user.war")) {
                    player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                warCommand.execute(player, playerClan, args);
                break;

            case "info":
                if (!player.hasPermission("pinkyteams.user.info")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                handleInfoCommand(player, playerClan, args);
                break;

            case "join":
                if (!player.hasPermission("pinkyteams.user.join")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_join")));
                    return true;
                }
                invitationCommand.join(player, args[1]);
                break;

            case "accept":
                if (!player.hasPermission("pinkyteams.user.accept")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_join")));
                    return true;
                }
                invitationCommand.join(player, args[1]);
                break;

            case "decline":
                if (!player.hasPermission("pinkyteams.user.decline")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.invite_decline_usage")));
                    return true;
                }
                invitationCommand.decline(player,args[1]);
                break;

            case "leave":
                if (!player.hasPermission("pinkyteams.user.leave")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                lifecycleCommand.leave(player,playerClan);
                break;

            case "kick":
                if (!player.hasPermission("pinkyteams.user.kick")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if(args.length!=2){player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_kick")));return true;}
                lifecycleCommand.kick(player,args[1]);
                break;

            case "invite":
                if (!player.hasPermission("pinkyteams.user.invite")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_invite")));
                    return true;
                }
                invitationCommand.invite(player, args[1]);
                break;

            case "chat":
                if (!player.hasPermission("pinkyteams.user.chat")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }

                if (playerClan == null || playerClan.isEmpty()) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }

                if (args.length >= 2) {
                    // Modo clásico: mensaje directo al clan.
                    queryCommand.chat(playerClan,player,String.join(" ",Arrays.copyOfRange(args,1,args.length)));
                } else {
                    // Modo toggle: activa o desactiva el chat del clan.
                    plugin.toggleClanChat(player);
                    boolean toggled = plugin.isClanChatToggled(player);
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix(
                        toggled ? "user.chat_enabled" : "user.chat_disabled")));
                }
                break;

            case "stats":
                if (!player.hasPermission("pinkyteams.user.stats")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }

                if (args.length >= 2) {
                    String targetClan = args[1];
                    queryCommand.stats(sender,targetClan);
                } else {
                    if (playerClan == null || playerClan.isEmpty()) {
                        sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                        return true;
                    }
                    queryCommand.stats(sender,playerClan);
                }
                break;

            case "resign":
                if (!player.hasPermission("pinkyteams.user.resign")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                lifecycleCommand.resign(player,playerClan);
                break;

            case "ff":
                if (!player.hasPermission("pinkyteams.user.ff")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }
                if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_ff")));
                    return true;
                }
                handleFriendlyFireCommand(sender, playerClan, args);
                break;

            case "ally":
                if (!player.hasPermission("pinkyteams.user.ally")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                if (playerClan == null || playerClan.isEmpty()) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.usage_ally")));
                    return true;
                }
                allianceCommand.execute(player,playerClan,args);
                break;

            case "edit":
                if (!player.hasPermission("pinkyteams.user.edit")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                editCommand.execute(player,playerClan,args);
                break;

            case "economy":
                if (!player.hasPermission("pinkyteams.user.economy")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                economyCommand.execute(player, playerClan, args);
                break;
            
            case "slots":
                if (!player.hasPermission("pinkyteams.user.slots")) {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_permission")));
                    return true;
                }
                slotCommand.execute(player, playerClan, args);
                break;

            default:
                if (showMainHelpPage) {
                    this.help(player, 1);
                } else {
                    sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.command_not_found")));
                }
                break;
        }

        return true;
    }





    public void help(Player player, int page) {
        int linesPerPage = 5;
        int totalPages = (int) Math.ceil((double) helpLines.size() / linesPerPage);

        if (page < 1 || page > totalPages) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.invalid_page_help")
                .replace("{total_pages}", String.valueOf(totalPages))));
            return;
        }

        player.sendMessage(MSG.color(langManager.getMessage("user.help_header")));
        player.sendMessage(MSG.color(langManager.getMessage("user.help_title")
            .replace("{page}", String.valueOf(page))
            .replace("{total_pages}", String.valueOf(totalPages))));
        player.sendMessage(MSG.color(langManager.getMessage("user.help_header")));

        int start = (page - 1) * linesPerPage;
        int end = Math.min(start + linesPerPage, helpLines.size());

        for (int i = start; i < end; i++) {
            player.sendMessage(MSG.color(helpLines.get(i)));
        }

        // Flechas de navegación.
        TextComponent nav = new TextComponent();

        if (page > 1) {
            TextComponent prev = new TextComponent(MSG.color(langManager.getMessage("user.help_previous_page")));
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan help " + (page - 1)));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text(langManager.getMessage("user.help_click_to_page").replace("{page}", String.valueOf(page - 1)))));
            nav.addExtra(prev);
        }

        if (page < totalPages) {
            TextComponent next = new TextComponent(MSG.color(langManager.getMessage("user.help_next_page")));
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clan help " + (page + 1)));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new Text(langManager.getMessage("user.help_click_to_page").replace("{page}", String.valueOf(page + 1)))));
            nav.addExtra(next);
        }

        player.spigot().sendMessage(nav);
        player.sendMessage(MSG.color(langManager.getMessage("user.help_footer")));
    }

    private String formatMoney(double amount) {
        return new DecimalFormat("#,##0.##").format(amount);
    }

    public void chat(String clanName, Player player, String[] message) {
        queryCommand.chat(clanName, player, String.join(" ", message));
    }

    private String getPlayerClan(String playerName) {
        return plugin.getStorageProvider().getPlayerClan(playerName);
    }

    private void handleInfoCommand(Player player, String playerClan, String[] args) {
        String targetClan = args.length >= 2 ? args[1] : playerClan;
        if (targetClan == null || targetClan.trim().isEmpty()) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }
        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().openMembersView(player, targetClan);
        }
    }

    private void handleFriendlyFireCommand(CommandSender sender, String playerClan, String[] args) {
        Player player = (Player) sender;

        if (!hasClanPermission(player, playerClan, ClanPermission.FF)) {
            return;
        }

        if (playerClan == null || playerClan.isEmpty()) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.no_clan")));
            return;
        }

        if (args.length != 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ff_usage")));
            return;
        }

        boolean enabled = args[1].equalsIgnoreCase("on");

        try {
            plugin.getStorageProvider().setFriendlyFireEnabled(playerClan, enabled);

            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ff_status")
                .replace("{status}", enabled ? langManager.getMessage("status.enabled") : langManager.getMessage("status.disabled"))));

        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(MSG.color(langManager.getMessageWithPrefix("command.ff_error")));
        }
    }

    private void handleTopCommand(Player player, String[] args) {
        if (args.length < 2) {
            if (plugin.getGuiManager() != null) {
                plugin.getGuiManager().openTopSelect(player);
                return;
            }
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.top_usage")));
            return;
        }

        TopMetric metric = TopMetric.fromKey(args[1]).orElse(null);
        if (metric == null) {
            player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.top_invalid_metric")));
            return;
        }

        int page = 1;
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.invalid_page_number")));
                return;
            }
        }

        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().openTopList(player, metric, page);
            return;
        }

        player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.top_usage")));
    }

    // ------------------------------------
    // Métodos auxiliares:

    private boolean shouldShowMainHelpPage() {
        return plugin.getFH().getConfig().getBoolean("commands.show-main-help-page", true);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return args.length == 1 ? List.of("reload") : new ArrayList<>();
        }

        String playerClan = PinkyTeams.getInstance().getStorageProvider().getCachedPlayerClan(player.getName());
        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1 -> completions.addAll(List.of(
                    "create", "disband", "report", "list", "info", "join", "accept", "decline",
                    "kick", "invite", "chat", "leave", "stats", "resign", "edit",
                    "ff", "ally", "war", "help", "home", "sethome", "delhome", "economy", "slots",
                    "top", "rank", "menu", "gui", "commands"
            ));

            case 2 -> {
                String arg0 = args[0].toLowerCase();
                switch (arg0) {
                    case "join", "accept", "decline" -> {
                        if (isNotInClan(playerClan)) completions.addAll(PinkyTeams.getInstance().getStorageProvider().getCachedClanNames());
                    }
                    case "invite" -> {
                        if (isInClan(playerClan) && hasClanPermissionSilent(player, playerClan, ClanPermission.INVITE)) {
                            completions.addAll(getOnlinePlayerNames());
                        }
                    }
                    case "kick" -> {
                        if (isInClan(playerClan) && hasClanPermissionSilent(player, playerClan, ClanPermission.KICK)) {
                            completions.addAll(getOnlinePlayerNames());
                        }
                    }
                    case "economy" -> completions.addAll(List.of("deposit", "withdraw", "history"));
                    case "war" -> completions.addAll(List.of("request", "accept", "decline", "status", "surrender"));
                    case "report", "allyremove" -> completions.addAll(PinkyTeams.getInstance().getStorageProvider().getCachedClanNames());
                    case "edit" -> {
                        if (isInClan(playerClan) && (
                            hasClanPermissionSilent(player, playerClan, ClanPermission.EDIT_NAME)
                                || hasClanPermissionSilent(player, playerClan, ClanPermission.EDIT_TAG)
                                || hasClanPermissionSilent(player, playerClan, ClanPermission.EDIT_PRIVACY)
                        )) {
                            completions.addAll(List.of("name", "tag", "privacy"));
                        }
                    }
                    case "ff" -> {
                        completions.addAll(List.of("on", "off"));
                    }
                    case "ally" -> {
                        completions.addAll(List.of("request", "accept", "decline", "remove", "ff"));
                    }
                    case "slots" -> completions.add("buy");
                    case "disband" -> completions.add("confirm");
                    case "top" -> completions.addAll(List.of("kda", "points", "money", "members"));
                    case "rank" -> completions.addAll(List.of("create", "delete", "set", "perms", "list"));
                }
            }

            case 3 -> {
                String arg0 = args[0].toLowerCase();
                String arg1 = args[1].toLowerCase();

                if (arg0.equals("ally")) {
                    if (arg1.equals("request")) {
                        completions.addAll(PinkyTeams.getInstance().getStorageProvider().getCachedClanNames());
                    } else if (arg1.equals("accept") || arg1.equals("decline")) {
                        completions.addAll(plugin.getStorageProvider().getPendingAlliances(playerClan));
                    } else if (arg1.equals("remove")) {
                        completions.addAll(plugin.getStorageProvider().getClanAlliances(playerClan));
                    } else if (arg1.equals("ff")) {
                        completions.addAll(List.of("on", "off"));
                    }
                } else if (arg0.equals("war") && (arg1.equals("request") || arg1.equals("accept") || arg1.equals("decline"))) {
                    completions.addAll(plugin.getStorageProvider().getCachedClanNames());
                } else if (arg0.equals("rank")) {
                    if (arg1.equals("set")) {
                        completions.addAll(getClanMemberNames(playerClan));
                    } else if (arg1.equals("delete") || arg1.equals("perms")) {
                        completions.addAll(getRoleNames(playerClan));
                    }
                }
            }

            case 4 -> {
                String arg0 = args[0].toLowerCase();
                String arg1 = args[1].toLowerCase();
                if (arg0.equals("rank")) {
                    if (arg1.equals("set")) {
                        completions.addAll(getRoleNames(playerClan));
                    } else if (arg1.equals("perms")) {
                        completions.addAll(List.of("list", "add", "remove", "clear"));
                    }
                }
            }

            case 5 -> {
                String arg0 = args[0].toLowerCase();
                String arg1 = args[1].toLowerCase();
                String arg3 = args[3].toLowerCase();
                if (arg0.equals("rank") && arg1.equals("perms") && (arg3.equals("add") || arg3.equals("remove"))) {
                    for (ClanPermission permission : ClanPermission.values()) {
                        completions.add(permission.getKey());
                    }
                }
            }
        }

        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private boolean isInClan(String clan) {
        return clan != null && !clan.isEmpty();
    }

    private boolean isNotInClan(String clan) {
        return !isInClan(clan);
    }

    private boolean isLeader(Player player, String clanName) {
        if (clanName == null || clanName.isEmpty()) {
            return false;
        }

        if (plugin.getRoleManager() != null && plugin.getRoleManager().isCoLeader(player, clanName)) {
            return true;
        }
        
        // Try to get the leader using StorageProvider
        String leader = plugin.getStorageProvider().getClanLeader(clanName);
        if (leader != null) {
            return player.getName().equalsIgnoreCase(leader);
        }
        
        // If not found, try with stripped color codes
        String plainName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', clanName));
        if (!plainName.equals(clanName)) {
            leader = plugin.getStorageProvider().getClanLeader(plainName);
            if (leader != null) {
                return player.getName().equalsIgnoreCase(leader);
            }
        }
        
        return false;
    }

    private String resolveClanName(String clanName) {
        if (clanName == null || clanName.trim().isEmpty()) {
            return clanName;
        }
        for (String stored : plugin.getStorageProvider().getCachedClanNames()) {
            if (stored.equalsIgnoreCase(clanName)) {
                return stored;
            }
        }
        return clanName;
    }

    private List<String> getClanNames() {
        return new ArrayList<>(plugin.getStorageProvider().getAllClans());
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> getClanMemberNames(String clanName) {
        if (clanName == null || clanName.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(plugin.getStorageProvider().getClanMembers(clanName));
    }

    private List<String> getRoleNames(String clanName) {
        if (clanName == null || clanName.isEmpty() || plugin.getRoleManager() == null) {
            return Collections.emptyList();
        }
        Map<String, Set<ClanPermission>> roles = plugin.getRoleManager().getClanRolePermissions(clanName);
        Set<String> names = new HashSet<>(roles.keySet());
        names.add(ClanRoleManager.ROLE_MEMBER);
        names.add(ClanRoleManager.ROLE_CO_LEADER);
        return new ArrayList<>(names);
    }

    private boolean hasClanPermissionSilent(Player player, String clanName, ClanPermission permission) {
        if (player == null || clanName == null || clanName.isEmpty() || permission == null) {
            return false;
        }
        if (plugin.getRoleManager() != null) {
            return plugin.getRoleManager().hasPermission(player, clanName, permission);
        }
        return isLeader(player, clanName);
    }
    private boolean hasClanPermission(Player player, String clanName, ClanPermission permission) {
        if (player == null || clanName == null || clanName.isEmpty() || permission == null) {
            return false;
        }
        if (plugin.getRoleManager() != null && plugin.getRoleManager().hasPermission(player, clanName, permission)) {
            return true;
        }
        player.sendMessage(MSG.color(langManager.getMessageWithPrefix("user.role_no_permission")));
        return false;
    }
}

