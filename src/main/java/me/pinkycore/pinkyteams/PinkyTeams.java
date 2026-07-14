package me.pinkycore.pinkyteams;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import me.pinkycore.pinkyteams.CMDs.CCMD;
import me.pinkycore.pinkyteams.CMDs.ACMD;
import me.pinkycore.pinkyteams.CMDs.PECMD;
import me.pinkycore.pinkyteams.CMDs.LangCMD;
import me.pinkycore.pinkyteams.Events.Events;
import me.pinkycore.pinkyteams.Utils.*;
import me.pinkycore.pinkyteams.Utils.ClanRoleManager;
import me.pinkycore.pinkyteams.Utils.ChatInputManager;
import me.pinkycore.pinkyteams.gui.ClanGuiManager;
import me.pinkycore.pinkyteams.Database.MariaDBManager;
import me.pinkycore.pinkyteams.Database.StorageProvider;
import me.pinkycore.pinkyteams.Database.StorageFactory;
import me.pinkycore.pinkyteams.listeners.PlayerStatsListener;
import me.pinkycore.pinkyteams.Utils.NameTagManager;
import me.pinkycore.pinkyteams.integration.DiscordNotifier;
import me.pinkycore.pinkyteams.integration.TabHook;
import me.pinkycore.pinkyteams.integration.UnlimitedNametagHook;
import me.pinkycore.pinkyteams.Utils.IpClanTracker;
import me.pinkycore.pinkyteams.service.ClanBankService;
import me.pinkycore.pinkyteams.service.ClanMembershipService;
import me.pinkycore.pinkyteams.service.ClanAllianceService;
import me.pinkycore.pinkyteams.service.PlayerIdentityService;
import me.pinkycore.pinkyteams.service.ClanWarService;
import me.pinkycore.pinkyteams.service.ClanSeasonService;
import me.pinkycore.pinkyteams.service.BankAuditService;
import me.pinkycore.pinkyteams.service.ClanSlotService;
import me.pinkycore.pinkyteams.api.PinkyTeamsAPI;
import me.pinkycore.pinkyteams.api.PinkyTeamsAPIImpl;
import me.pinkycore.pinkyteams.api.event.ClanWarEndEvent;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.util.Objects;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;



public class PinkyTeams extends JavaPlugin {
   public String version = getDescription().getVersion();
   public static String prefix;
   public static Econo econ;

   private Updater updater;
   private Metrics metrics;
   private FileHandler fh;
   private StorageProvider storageProvider;
   public LangManager langManager;
   private LangCMD langCMD;
   private CCMD ccCmd;
   private NameTagManager nameTagManager;
   private TabHook tabHook;
   private ClanRoleManager roleManager;
   private ChatInputManager chatInputManager;
   private ClanGuiManager guiManager;
   private ClanBankService clanBankService;
   private ClanMembershipService clanMembershipService;
   private ClanAllianceService clanAllianceService;
   private PlayerIdentityService playerIdentityService;
   private ClanWarService clanWarService;
   private ClanSeasonService clanSeasonService;
   private BankAuditService bankAuditService;
   private ClanSlotService clanSlotService;
   private PinkyTeamsAPI api;

   private IpClanTracker ipClanTracker;
   private DiscordNotifier discordNotifier;
   private UnlimitedNametagHook unlimitedNametagHook;

   private static PinkyTeams instance;

   public Set<UUID> teleportingPlayers = ConcurrentHashMap.newKeySet();
   public Map<UUID, Long> homeCooldowns = new ConcurrentHashMap<>();
   private final Set<UUID> clanChatToggled = ConcurrentHashMap.newKeySet();
   private final Set<String> registeredClanCustomCommands = new HashSet<>();
   public int clanHomeCooldown;
   public int clanHomeDelay;
   

   @Override
   public void onEnable() {
      instance = this;
      LegacyDataMigrator.migrateIfNeeded(this);
      saveDefaultConfig();
      ConfigMigrator.migrate(this);
      disableLegacyPluginAfterMigration();
      this.clanHomeCooldown = getConfig().getInt("clan_home.cooldown", 30);
      this.clanHomeDelay = getConfig().getInt("clan_home.teleport_delay", 5);
      prefix = getConfig().getString("prefix", "&7[&d&lᴘɪɴᴋʏᴛᴇᴀᴍꜱ&7]");
      fh = new FileHandler(this);
      ipClanTracker = new IpClanTracker(this);
      updater = new Updater(this, 126207);
      metrics = new Metrics(this, 20912);
      econ = new Econo(this);
      ClanUtils.init(this);
      copyLangFiles();
      langManager = new LangManager(this);
      discordNotifier = new DiscordNotifier(this);
      LangCMD langCMD = new LangCMD(this);
      getServer().getPluginManager().registerEvents(new PlayerStatsListener(this), this);
      setLangCMD(langCMD);

      this.ccCmd = new CCMD(this, langManager);
      getCommand("clan").setExecutor(ccCmd);

      if (getConfig().getBoolean("economy.enabled", true)) {
         if (!econ.setupEconomy()) {
            getLogger().severe("Can´t load the economy system.");
            fh.getConfig().set("economy.enabled", false);
            fh.saveConfig();
            getLogger().severe("Economy system disabled.");
            return;
         }
      }

      fh.saveDefaults();

      try {
         String storageType = getConfig().getString("storage.type", "yaml");
         storageProvider = StorageFactory.createStorageProvider(storageType, getConfig());
         getLogger().info("Storage provider initialized: " + storageType);
      } catch (Exception e) {
         getLogger().severe("Failed to initialize storage provider: " + e.getMessage());
         getLogger().severe("Falling back to YAML storage...");
         try {
            storageProvider = StorageFactory.createStorageProvider("yaml", getConfig());
            getLogger().info("YAML storage provider initialized as fallback.");
         } catch (Exception fallbackError) {
            getLogger().severe("Failed to initialize fallback storage: " + fallbackError.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
         }
      }

      roleManager = new ClanRoleManager(this);
      clanBankService = new ClanBankService(storageProvider, econ);
      clanMembershipService = new ClanMembershipService(storageProvider);
      clanAllianceService = new ClanAllianceService(storageProvider);
      playerIdentityService = new PlayerIdentityService(this);
      clanWarService = new ClanWarService(this);
      clanSeasonService = new ClanSeasonService(this);
      bankAuditService = new BankAuditService(this);
      clanSlotService = new ClanSlotService(this);
      Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
         for (ClanWarService.WarResult result : clanWarService.pollTimedResults()) {
            completeWar(result);
         }
      }, 20L, 20L);
      Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
         if (clanSeasonService.isExpired()) clanSeasonService.end(
            getConfig().getBoolean("seasons.reset-points-on-end", true)).ifPresent(this::completeSeason);
      }, 1200L, 1200L);
      api = new PinkyTeamsAPIImpl(storageProvider);
      chatInputManager = new ChatInputManager(this);
      guiManager = new ClanGuiManager(this);

      nameTagManager = new NameTagManager(this);

      tabHook = new TabHook(this);
      unlimitedNametagHook = new UnlimitedNametagHook(this);
      String nametagProvider = getConfig().getString("nametag-privacy.provider", "internal");
      refreshNametagProviders();

      setupMetrics();
      Bukkit.getScheduler().runTaskTimerAsynchronously(this,
         () -> new ClanTopCalculator(this).refreshAll(), 20L, 600L);
      registerCommands();
      registerEvents();
      searchUpdates();
      discordNotifier.reload();

      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
         new PAPI(this).registerPlaceholders();
         if (getConfig().getBoolean("compatibility.legacy-placeholders", true)) {
            LegacyPAPI legacyPAPI = new LegacyPAPI(this);
            if (legacyPAPI.register()) {
               getLogger().warning("Legacy %vanguardclans_*% placeholders are enabled for compatibility. Migrate them to %pinkyteams_*%.");
            }
         }
         getLogger().info("Placeholders de PinkyTeams registrados correctamente.");
      }

      if (!"tab".equalsIgnoreCase(nametagProvider)) {
         getLogger().info("TAB no usado como proveedor de nametag (provider=" + nametagProvider + "); se usara el gestor interno si esta habilitado.");
      } else {
         getLogger().info("TAB usado como proveedor de nametag (provider=tab).");
      }

      // Enhanced startup message
      Bukkit.getConsoleSender().sendMessage(MSG.color("&2&l============================================================"));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&d&l    ᴘɪɴᴋʏᴛᴇᴀᴍꜱ &fEnabled Successfully!"));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&7"));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lVersion: &f" + getDescription().getVersion()));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lAuthor: &f" + String.join(", ", getDescription().getAuthors())));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lStorage Type: &f" + getConfig().getString("storage.type", "yaml").toUpperCase()));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lEconomy: &f" + (getConfig().getBoolean("economy.enabled", true) ? "&aEnabled" : "&cDisabled")));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lPlaceholderAPI: &f" + (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null ? "&aHooked" : "&cNot Found")));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lMax Clans: &f" + (getConfig().getInt("max-clans", -1) <= 0 ? "Unlimited" : getConfig().getInt("max-clans"))));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&7"));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&2&l============================================================"));
   }

   private void disableLegacyPluginAfterMigration() {
      if (!getConfig().getBoolean("compatibility.disable-legacy-plugin-after-migration", true)
          || !LegacyDataMigrator.isMigrationComplete(this)) return;

      org.bukkit.plugin.Plugin legacy = getServer().getPluginManager().getPlugin("VanguardClans");
      if (legacy == null || legacy == this) return;

      // Run after the enable phase so plugin load order cannot re-enable the legacy instance afterward.
      getServer().getScheduler().runTask(this, () -> {
         if (legacy.isEnabled()) {
            getLogger().warning("Successful migration detected. Disabling VanguardClans to prevent both plugins from modifying clan data.");
            getServer().getPluginManager().disablePlugin(legacy);
         }
         getLogger().info("VanguardClans is disabled for this server session. Remove its old JAR before the next restart.");
      });
   }

   private void refreshNametagProviders() {
      String provider = getConfig().getString("nametag-privacy.provider", "internal");
      if ("tab".equalsIgnoreCase(provider) && Bukkit.getPluginManager().isPluginEnabled("TAB")) {
         tabHook.start();
      } else {
         tabHook.stop();
      }
      if ("unt".equalsIgnoreCase(provider) && Bukkit.getPluginManager().isPluginEnabled("UnlimitedNameTags")) {
         unlimitedNametagHook.start();
      } else {
         unlimitedNametagHook.stop();
      }
   }

   public void reloadIntegrations() {
      if (ipClanTracker != null) {
         ipClanTracker.reload();
      }
      if (discordNotifier != null) {
         discordNotifier.reload();
      }
      refreshNametagProviders();
   }


   @Override
   public void onDisable() {
      if (storageProvider != null) {
         storageProvider.close();
         getLogger().info("Storage provider closed successfully.");
      }
      if (nameTagManager != null) {
         nameTagManager.shutdown();
      }
      if (tabHook != null) {
         tabHook.stop();
      }
      if (unlimitedNametagHook != null) {
         unlimitedNametagHook.stop();
      }
      if (discordNotifier != null) {
         discordNotifier.stopWatcher();
      }
      
      // Enhanced shutdown message
      Bukkit.getConsoleSender().sendMessage(MSG.color("&c&l============================================================"));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&d&l    ᴘɪɴᴋʏᴛᴇᴀᴍꜱ &fDisabled!"));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&7"));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lVersion: &f" + getDescription().getVersion()));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&7&lAll clan data has been saved."));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&7Thank you for using &d&lᴘɪɴᴋʏᴛᴇᴀᴍꜱ&7!"));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&7"));
      Bukkit.getConsoleSender().sendMessage(MSG.color("&c&l============================================================"));
   }

   public static PinkyTeams getInstance() {
      return instance;
   }

   private void copyLangFiles() {
      File langFolder = new File(getDataFolder(), "lang");
      if (!langFolder.exists()) {
         langFolder.mkdirs();
      }

      String[] languages = {"es.yml", "en.yml"};  // pon aquí todos los idiomas que tengas

      for (String langFile : languages) {
         File file = new File(langFolder, langFile);
         if (!file.exists()) {
               saveResource("lang/" + langFile, false);
         }
      }
   }


   private void setupMetrics() {
      int max = getConfig().getInt("max-clans", -1);
      String maxClans = (max <= 0) ? "Unlimited" : String.valueOf(max);

      metrics.addCustomChart(new Metrics.SimplePie("economy_enabled",
              () -> String.valueOf(getConfig().getBoolean("economy.enabled", true))));
      metrics.addCustomChart(new Metrics.SimplePie("economy_system",
              () -> getConfig().getString("economy.system", "Unknown")));
      metrics.addCustomChart(new Metrics.SimplePie("max_clans",
              () -> maxClans));
   }

   private void registerCommands() {
      Objects.requireNonNull(getCommand("clanadmin")).setExecutor(new ACMD(this));
      refreshClanCommand();
      Objects.requireNonNull(getCommand("scstats")).setExecutor(new PECMD(this));
   }

   public void refreshClanCommand() {
      this.ccCmd = new CCMD(this, langManager);
      PluginCommand clanCommand = Objects.requireNonNull(getCommand("clan"));
      clanCommand.setExecutor(ccCmd);
      clanCommand.setTabCompleter(ccCmd);
      registerClanAliases(clanCommand);
   }

   private void registerClanAliases(PluginCommand clanCommand) {
      List<String> aliases = getConfiguredClanCommandAliases();
      unregisterClanCustomCommands();
      if (aliases.isEmpty()) {
         return;
      }

      clanCommand.setAliases(aliases);
      try {
         CommandMap commandMap = getCommandMap();
         Map<String, Command> knownCommands = getKnownCommands(commandMap);
         for (String alias : aliases) {
            if (alias == null || alias.trim().isEmpty()) {
               continue;
            }

            String normalized = alias.trim().toLowerCase(Locale.ROOT);
            if ("clan".equals(normalized)) {
               continue;
            }

            Command existing = knownCommands.get(normalized);
            if (existing != null && existing != clanCommand && !(existing instanceof ClanCustomCommand)) {
               getLogger().warning("Cannot register /" + normalized + " as clan custom command because another command already uses it.");
               continue;
            }

            ClanCustomCommand customCommand = new ClanCustomCommand(normalized, clanCommand);
            commandMap.register(getName().toLowerCase(Locale.ROOT), customCommand);
            knownCommands.put(normalized, customCommand);
            knownCommands.put(getName().toLowerCase(Locale.ROOT) + ":" + normalized, customCommand);
            registeredClanCustomCommands.add(normalized);
         }
      } catch (ReflectiveOperationException e) {
         getLogger().warning("Could not register clan custom commands: " + e.getMessage());
      }
   }

   private List<String> getConfiguredClanCommandAliases() {
      Set<String> aliases = new HashSet<>();
      aliases.addAll(getConfig().getStringList("commands.clan-aliases"));

      List<String> normalized = new ArrayList<>();
      for (String alias : aliases) {
         if (alias == null || alias.trim().isEmpty()) {
            continue;
         }
         String clean = alias.trim().toLowerCase(Locale.ROOT);
         if (clean.startsWith("/")) {
            clean = clean.substring(1);
         }
         if (!clean.isEmpty() && !normalized.contains(clean)) {
            normalized.add(clean);
         }
      }
      return normalized;
   }

   private void unregisterClanCustomCommands() {
      if (registeredClanCustomCommands.isEmpty()) {
         return;
      }
      try {
         Map<String, Command> knownCommands = getKnownCommands(getCommandMap());
         for (String alias : registeredClanCustomCommands) {
            Command command = knownCommands.get(alias);
            if (command instanceof ClanCustomCommand) {
               knownCommands.remove(alias);
            }
            String namespaced = getName().toLowerCase(Locale.ROOT) + ":" + alias;
            command = knownCommands.get(namespaced);
            if (command instanceof ClanCustomCommand) {
               knownCommands.remove(namespaced);
            }
         }
      } catch (ReflectiveOperationException e) {
         getLogger().warning("Could not unregister previous clan custom commands: " + e.getMessage());
      }
      registeredClanCustomCommands.clear();
   }

   private CommandMap getCommandMap() throws ReflectiveOperationException {
      java.lang.reflect.Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
      commandMapField.setAccessible(true);
      return (CommandMap) commandMapField.get(Bukkit.getServer());
   }

   @SuppressWarnings("unchecked")
   private Map<String, Command> getKnownCommands(CommandMap commandMap) throws ReflectiveOperationException {
      java.lang.reflect.Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
      knownCommandsField.setAccessible(true);
      return (Map<String, Command>) knownCommandsField.get(commandMap);
   }

   private final class ClanCustomCommand extends Command {
      private final PluginCommand clanCommand;

      private ClanCustomCommand(String name, PluginCommand clanCommand) {
         super(name);
         this.clanCommand = clanCommand;
         setDescription("Custom command for PinkyTeams /clan.");
         setUsage("/" + name + " <subcommand>");
      }

      @Override
      public boolean execute(CommandSender sender, String commandLabel, String[] args) {
         return ccCmd != null && ccCmd.onCommand(sender, clanCommand, commandLabel, args);
      }

      @Override
      public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
         if (ccCmd == null) {
            return Collections.emptyList();
         }
         return ccCmd.onTabComplete(sender, clanCommand, alias, args);
      }
   }

   private void registerEvents() {
      getServer().getPluginManager().registerEvents(new Events(this, ccCmd), this);
      getServer().getPluginManager().registerEvents(guiManager, this);
   }

   public void searchUpdates() {
      String downloadUrl = "https://www.spigotmc.org/resources/pinkyteams-advanced-clans-system.126207";
      TextComponent link = new TextComponent(MSG.color("&6&lClick here to download the update!"));
      link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl));

      boolean updateAvailable = false;
      String latestVersion = "unknown";

      try {
         updater = new Updater(this, 126207);
         updateAvailable = updater.isUpdateAvailable();
         latestVersion = updater.getLatestVersion();
      } catch (Exception e) {
         Bukkit.getConsoleSender().sendMessage(MSG.color("&cError checking for updates: " + e.getMessage()));
      }

      if (updateAvailable) {
          Bukkit.getConsoleSender().sendMessage(MSG.color("&2&l============================================================"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&6&l         ＮＥＷ  ＶＥＲＳＩＯＮ  ＡＶＡＩＬＡＢＬＥ!"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&7"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&d&lPlugin: &fᴘɪɴᴋʏᴛᴇᴀᴍꜱ"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lCurrent Version: &f" + version));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lLatest Version: &f" + latestVersion));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&e&lDownload: &b" + downloadUrl));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&7"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&a&lChangelog &7(see plugin page for details)"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&7- Bug fixes and improvements"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&7- New features may be available!"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&7"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&c&lPlease update to enjoy the latest features and fixes!"));
          Bukkit.getConsoleSender().sendMessage(MSG.color("&2&l============================================================"));

         for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("pinkyteams.admin")) {
                  player.sendMessage(MSG.color(prefix + "&e A new plugin update is available!"));
               player.spigot().sendMessage(link);
            }
         }
      }
   }

   public boolean isClanBanned(String clanName) {
      return getStorageProvider().isClanBanned(clanName);
   }

   public boolean isClanChatToggled(Player player) {
      return clanChatToggled.contains(player.getUniqueId());
   }

   public void toggleClanChat(Player player) {
      if (isClanChatToggled(player)) {
         clanChatToggled.remove(player.getUniqueId());
      } else {
         clanChatToggled.add(player.getUniqueId());
      }
   }


   public String getPlayerClan(String playerName) {
      if (playerName == null || playerName.isEmpty()) {
         return null;
      }
      
      return this.getStorageProvider().getPlayerClan(playerName);
   }



   public boolean isWorldBlocked(World world) {
      return getConfig().getStringList("blocked-worlds").contains(world.getName());
   }

   public static Econo getEcon() {
      return econ;
   }

   public FileHandler getFH() {
      return fh;
   }

   public StorageProvider getStorageProvider() {
      return storageProvider;
   }

   public NameTagManager getNameTagManager() {
      return nameTagManager;
   }

   public ClanRoleManager getRoleManager() {
      return roleManager;
   }

   public ClanBankService getClanBankService() {
      return clanBankService;
   }

   public ClanMembershipService getClanMembershipService() {
      return clanMembershipService;
   }

   public ClanAllianceService getClanAllianceService() {
      return clanAllianceService;
   }

   public PlayerIdentityService getPlayerIdentityService() {
      return playerIdentityService;
   }

   public ClanWarService getClanWarService() {
      return clanWarService;
   }

   public ClanSeasonService getClanSeasonService() { return clanSeasonService; }
   public BankAuditService getBankAuditService() { return bankAuditService; }
   public ClanSlotService getClanSlotService() { return clanSlotService; }

   public void completeSeason(ClanSeasonService.SeasonResult result) {
      Bukkit.getScheduler().runTask(this, () -> {
         int max=Math.min(getConfig().getInt("seasons.ranking-size",10),result.ranking().size());
         for(int i=0;i<max;i++){
            var entry=result.ranking().get(i); int position=i+1;
            String leader=Objects.toString(getStorageProvider().getClanLeader(entry.getClanName()),"");
            for(String command:getConfig().getStringList("seasons.rewards."+position)){
               Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command.replace("{clan}",entry.getClanName())
                  .replace("{leader}",leader).replace("{position}",String.valueOf(position))
                  .replace("{points}",String.valueOf(entry.getPoints())));
            }
         }
         Bukkit.broadcastMessage(MSG.color(prefix+" &dSeason &f"+result.name()+" &dhas ended."));
      });
   }

   public void completeWar(ClanWarService.WarResult result) {
      Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
         double reward = Math.max(0, getConfig().getDouble("wars.winner-bank-reward", 0));
         if (result.winner() != null && reward > 0) {
            var credit=clanBankService.creditClan(result.winner(), reward);
            if(credit.successful()) bankAuditService.record(result.winner(),"SYSTEM",BankAuditService.Type.WAR_REWARD,reward,credit.balance());
         }
         Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.getPluginManager().callEvent(new ClanWarEndEvent(result));
            String outcome = result.winner() == null ? "Draw" : result.winner() + " won";
            Bukkit.broadcastMessage(MSG.color(prefix + " &dWar finished: &f" + outcome + "&7."));
         });
      });
   }

   public PinkyTeamsAPI getApi() {
      return api;
   }

   public ChatInputManager getChatInputManager() {
      return chatInputManager;
   }

   public ClanGuiManager getGuiManager() {
       return guiManager;
   }

   public IpClanTracker getIpClanTracker() {
      return ipClanTracker;
   }

   public DiscordNotifier getDiscordNotifier() {
      return discordNotifier;
   }

   public void refreshClanVisuals() {
      if (nameTagManager != null) {
         nameTagManager.resetTeams();
      }
      if (tabHook != null) {
         tabHook.stop();
         tabHook.start();
      }
      if (unlimitedNametagHook != null) {
         unlimitedNametagHook.stop();
         unlimitedNametagHook.start();
      }
   }

   public void notifyClanDeleted(String clanName) {
      if (clanName == null) {
         return;
      }
      if (ipClanTracker != null) {
         ipClanTracker.removeClan(clanName);
      }
   }

   public void notifyClanRenamed(String oldName, String newName) {
      if (oldName == null || newName == null) {
         return;
      }
      if (ipClanTracker != null) {
         ipClanTracker.renameClan(oldName, newName);
      }
   }

   // Legacy method for backward compatibility
   public MariaDBManager getMariaDBManager() {
      if (storageProvider instanceof MariaDBManager) {
         return (MariaDBManager) storageProvider;
      }
      throw new UnsupportedOperationException("Current storage provider is not MariaDB");
   }

   public LangManager getLangManager() {
      return langManager;
   }

   public LangCMD getLangCMD() {
      return langCMD;
   }

   public void setLangCMD(LangCMD langCMD) {
      this.langCMD = langCMD;
   }
}
