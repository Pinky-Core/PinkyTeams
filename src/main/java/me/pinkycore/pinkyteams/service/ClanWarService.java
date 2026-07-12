package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.PinkyTeams;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClanWarService {
    private final PinkyTeams plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, Request> requests = new ConcurrentHashMap<>();
    private final Map<String, War> wars = new ConcurrentHashMap<>();
    private final Map<String, Long> killCooldowns = new ConcurrentHashMap<>();
    private final Queue<WarResult> timedResults = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public ClanWarService(PinkyTeams plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "wars.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public synchronized Result request(String challenger, String target) {
        cleanup();
        if (invalid(challenger, target) || challenger.equalsIgnoreCase(target)) return Result.INVALID;
        if (!plugin.getStorageProvider().clanExists(target)) return Result.CLAN_NOT_FOUND;
        if (findWar(challenger).isPresent() || findWar(target).isPresent()) return Result.ALREADY_AT_WAR;
        String key = pair(challenger, target);
        if (requests.containsKey(key)) return Result.ALREADY_PENDING;
        long expires = System.currentTimeMillis() + plugin.getConfig().getLong("wars.request-expiration-seconds", 300) * 1000L;
        requests.put(key, new Request(challenger, target, expires));
        save();
        return Result.SUCCESS;
    }

    public synchronized Result accept(String target, String challenger) {
        cleanup();
        Request request = requests.get(pair(challenger, target));
        if (request == null || !request.challenger.equalsIgnoreCase(challenger)
            || !request.target.equalsIgnoreCase(target)) return Result.NO_PENDING;
        if (findWar(challenger).isPresent() || findWar(target).isPresent()) return Result.ALREADY_AT_WAR;
        requests.remove(pair(challenger, target));
        long now = System.currentTimeMillis();
        long ends = now + plugin.getConfig().getLong("wars.duration-seconds", 3600) * 1000L;
        wars.put(pair(challenger, target), new War(challenger, target, 0, 0, now, ends));
        save();
        return Result.SUCCESS;
    }

    public synchronized Result decline(String target, String challenger) {
        Request request = requests.get(pair(challenger, target));
        if (request == null || !request.target.equalsIgnoreCase(target)) return Result.NO_PENDING;
        requests.remove(pair(challenger, target));
        save();
        return Result.SUCCESS;
    }

    public synchronized Optional<WarResult> surrender(String clan) {
        Optional<Map.Entry<String, War>> entry = wars.entrySet().stream()
            .filter(e -> e.getValue().contains(clan)).findFirst();
        if (entry.isEmpty()) return Optional.empty();
        War war = entry.get().getValue();
        wars.remove(entry.get().getKey());
        String winner = war.clanA.equalsIgnoreCase(clan) ? war.clanB : war.clanA;
        WarResult result = new WarResult(winner, clan, war.scoreA, war.scoreB, true);
        archive(result, "surrender");
        save();
        return Optional.of(result);
    }

    public synchronized Optional<WarResult> recordKill(String killerClan, String victimClan,
                                                        UUID killer, UUID victim, String address) {
        cleanup();
        Optional<Map.Entry<String, War>> entry = wars.entrySet().stream()
            .filter(e -> e.getValue().opponents(killerClan, victimClan)).findFirst();
        if (entry.isEmpty()) return Optional.empty();
        long cooldown = plugin.getConfig().getLong("wars.anti-farm.cooldown-seconds", 300) * 1000L;
        String farmKey = killer + ":" + victim + ":" + (address == null ? "" : address);
        long now = System.currentTimeMillis();
        if (killCooldowns.getOrDefault(farmKey, 0L) > now) return Optional.empty();
        killCooldowns.put(farmKey, now + cooldown);

        War old = entry.get().getValue();
        War updated = old.clanA.equalsIgnoreCase(killerClan)
            ? new War(old.clanA, old.clanB, old.scoreA + 1, old.scoreB, old.startedAt, old.endsAt)
            : new War(old.clanA, old.clanB, old.scoreA, old.scoreB + 1, old.startedAt, old.endsAt);
        wars.put(entry.get().getKey(), updated);
        int targetScore = plugin.getConfig().getInt("wars.score-to-win", 20);
        if (targetScore > 0 && Math.max(updated.scoreA, updated.scoreB) >= targetScore) {
            wars.remove(entry.get().getKey());
            WarResult result = result(updated, false);
            archive(result, "score");
            save();
            return Optional.of(result);
        }
        save();
        return Optional.empty();
    }

    public Optional<War> findWar(String clan) {
        cleanup();
        return wars.values().stream().filter(w -> w.contains(clan)).findFirst();
    }

    public List<Request> pendingFor(String clan) {
        cleanup();
        return requests.values().stream().filter(r -> r.target.equalsIgnoreCase(clan)).toList();
    }

    public List<WarResult> pollTimedResults() {
        cleanup();
        List<WarResult> results = new ArrayList<>();
        WarResult result;
        while ((result = timedResults.poll()) != null) results.add(result);
        return results;
    }

    private synchronized void cleanup() {
        long now = System.currentTimeMillis();
        boolean changed = requests.values().removeIf(r -> r.expiresAt <= now);
        Iterator<Map.Entry<String, War>> iterator = wars.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, War> entry = iterator.next();
            if (entry.getValue().endsAt <= now) {
                WarResult result = result(entry.getValue(), false);
                iterator.remove(); timedResults.add(result); archive(result, "time"); changed = true;
            }
        }
        killCooldowns.entrySet().removeIf(e -> e.getValue() <= now);
        if (changed) save();
    }

    private WarResult result(War war, boolean surrendered) {
        String winner = war.scoreA == war.scoreB ? null : war.scoreA > war.scoreB ? war.clanA : war.clanB;
        String loser = winner == null ? null : winner.equalsIgnoreCase(war.clanA) ? war.clanB : war.clanA;
        return new WarResult(winner, loser, war.scoreA, war.scoreB, surrendered);
    }

    private void archive(WarResult result, String reason) {
        String path = "history." + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        data.set(path + ".winner", result.winner); data.set(path + ".loser", result.loser);
        data.set(path + ".score-a", result.scoreA); data.set(path + ".score-b", result.scoreB);
        data.set(path + ".surrendered", result.surrendered); data.set(path + ".reason", reason);
        data.set(path + ".finished-at", System.currentTimeMillis());
    }

    private void load() {
        ConfigurationSection pending = data.getConfigurationSection("requests");
        if (pending != null) for (String key : pending.getKeys(false)) requests.put(key, new Request(
            pending.getString(key + ".challenger"), pending.getString(key + ".target"), pending.getLong(key + ".expires-at")));
        ConfigurationSection active = data.getConfigurationSection("active");
        if (active != null) for (String key : active.getKeys(false)) wars.put(key, new War(
            active.getString(key + ".clan-a"), active.getString(key + ".clan-b"), active.getInt(key + ".score-a"),
            active.getInt(key + ".score-b"), active.getLong(key + ".started-at"), active.getLong(key + ".ends-at")));
        cleanup();
    }

    private synchronized void save() {
        data.set("requests", null); data.set("active", null);
        requests.forEach((key, r) -> { data.set("requests."+key+".challenger",r.challenger); data.set("requests."+key+".target",r.target); data.set("requests."+key+".expires-at",r.expiresAt); });
        wars.forEach((key, w) -> { String p="active."+key; data.set(p+".clan-a",w.clanA); data.set(p+".clan-b",w.clanB); data.set(p+".score-a",w.scoreA); data.set(p+".score-b",w.scoreB); data.set(p+".started-at",w.startedAt); data.set(p+".ends-at",w.endsAt); });
        try { data.save(file); } catch (IOException e) { plugin.getLogger().severe("Could not save wars.yml: "+e.getMessage()); }
    }

    private String pair(String a,String b){ return a.compareToIgnoreCase(b)<=0 ? safe(a)+"__"+safe(b) : safe(b)+"__"+safe(a); }
    private String safe(String value){ return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_"); }
    private boolean invalid(String a,String b){ return a==null||b==null||a.isBlank()||b.isBlank(); }
    public enum Result { SUCCESS, INVALID, CLAN_NOT_FOUND, ALREADY_AT_WAR, ALREADY_PENDING, NO_PENDING }
    public record Request(String challenger,String target,long expiresAt) {}
    public record War(String clanA,String clanB,int scoreA,int scoreB,long startedAt,long endsAt) {
        public boolean contains(String clan){ return clanA.equalsIgnoreCase(clan)||clanB.equalsIgnoreCase(clan); }
        public boolean opponents(String a,String b){ return contains(a)&&contains(b)&&!a.equalsIgnoreCase(b); }
        public String opponentOf(String clan){ return clanA.equalsIgnoreCase(clan)?clanB:clanA; }
        public int scoreFor(String clan){ return clanA.equalsIgnoreCase(clan)?scoreA:scoreB; }
    }
    public record WarResult(String winner,String loser,int scoreA,int scoreB,boolean surrendered) {}
}
