package me.pinkycore.pinkyteams.Utils;

import me.pinkycore.pinkyteams.Database.StorageProvider;
import me.pinkycore.pinkyteams.PinkyTeams;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;

public class ClanTopCalculator {

    private final PinkyTeams plugin;
    private static final Map<TopMetric, CacheEntry> SHARED_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_MS = 30_000L;

    public ClanTopCalculator(PinkyTeams plugin) {
        this.plugin = plugin;
    }

    public List<ClanTopEntry> getTopEntries(TopMetric metric) {
        CacheEntry cached=SHARED_CACHE.get(metric);
        if(cached!=null&&System.currentTimeMillis()-cached.createdAt<CACHE_MS)return cached.entries;
        return refresh(metric);
    }

    public List<ClanTopEntry> refresh(TopMetric metric) {
        StorageProvider storage = plugin.getStorageProvider();
        Set<String> clans = storage.getAllClans();
        List<ClanTopEntry> entries = new ArrayList<>();

        for (String clan : clans) {
            String coloredName = storage.getClanColoredName(clan);
            int members = storage.getClanMemberCount(clan);
            int points = storage.getClanPoints(clan);
            double money = storage.getClanMoney(clan);

            int kills = 0;
            int deaths = 0;
            double kdaSum = 0.0;
            int memberCount = 0;

            List<String> clanMembers = storage.getClanMembers(clan);
            for (String member : clanMembers) {
                int playerKills = storage.getPlayerKills(member);
                int playerDeaths = storage.getPlayerDeaths(member);
                kills += playerKills;
                deaths += playerDeaths;
                double playerKda = playerDeaths == 0 ? playerKills : (double) playerKills / playerDeaths;
                kdaSum += playerKda;
                memberCount++;
            }

            double totalKda = deaths == 0 ? kills : (double) kills / deaths;
            double averageKda = memberCount == 0 ? 0.0 : kdaSum / memberCount;

            entries.add(new ClanTopEntry(
                clan,
                coloredName,
                members,
                points,
                money,
                kills,
                deaths,
                totalKda,
                averageKda
            ));
        }

        entries.sort(Comparator.comparingDouble((ClanTopEntry entry) -> entry.getSortValue(metric)).reversed());
        List<ClanTopEntry> snapshot=List.copyOf(entries);SHARED_CACHE.put(metric,new CacheEntry(snapshot,System.currentTimeMillis()));return snapshot;
    }

    public void refreshAll(){for(TopMetric metric:TopMetric.values())refresh(metric);}
    private record CacheEntry(List<ClanTopEntry> entries,long createdAt){}
}
