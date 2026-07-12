package me.pinkycore.pinkyteams.service;

import me.pinkycore.pinkyteams.PinkyTeams;
import me.pinkycore.pinkyteams.Utils.ClanTopCalculator;
import me.pinkycore.pinkyteams.Utils.ClanTopEntry;
import me.pinkycore.pinkyteams.Utils.TopMetric;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ClanSeasonService {
    private final PinkyTeams plugin;
    private final File file;
    private final YamlConfiguration data;

    public ClanSeasonService(PinkyTeams plugin) {
        this.plugin=plugin; this.file=new File(plugin.getDataFolder(),"seasons.yml");
        this.data=YamlConfiguration.loadConfiguration(file);
    }

    public synchronized boolean start(String name,long durationSeconds) {
        if (isActive() || name==null || name.isBlank() || durationSeconds<=0) return false;
        long now=System.currentTimeMillis(); data.set("current.name",name); data.set("current.started-at",now);
        data.set("current.ends-at",now+durationSeconds*1000L); data.set("current.active",true); save(); return true;
    }

    public synchronized Optional<SeasonResult> end(boolean resetPoints) {
        if(!isActive()) return Optional.empty();
        String name=data.getString("current.name","Season"); long started=data.getLong("current.started-at");
        List<ClanTopEntry> ranking=new ClanTopCalculator(plugin).getTopEntries(TopMetric.POINTS);
        String id=System.currentTimeMillis()+"-"+name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","_");
        data.set("history."+id+".name",name); data.set("history."+id+".started-at",started);
        data.set("history."+id+".ended-at",System.currentTimeMillis());
        List<Map<String,Object>> rows=new ArrayList<>(); int position=1;
        for(ClanTopEntry entry:ranking){ Map<String,Object> row=new LinkedHashMap<>(); row.put("position",position++);
            row.put("clan",entry.getClanName()); row.put("points",entry.getPoints()); row.put("kills",entry.getKills());
            row.put("deaths",entry.getDeaths()); rows.add(row); }
        data.set("history."+id+".ranking",rows); data.set("current",null); save();
        if(resetPoints) for(String clan:plugin.getStorageProvider().getAllClans()) plugin.getStorageProvider().setClanPoints(clan,0);
        plugin.getStorageProvider().reloadCache(); return Optional.of(new SeasonResult(id,name,List.copyOf(ranking)));
    }

    public boolean isActive(){return data.getBoolean("current.active",false);}
    public String name(){return isActive()?data.getString("current.name","Season"):"N/A";}
    public long timeLeftSeconds(){return isActive()?Math.max(0,(data.getLong("current.ends-at")-System.currentTimeMillis())/1000):0;}
    public boolean isExpired(){return isActive()&&data.getLong("current.ends-at")<=System.currentTimeMillis();}
    public synchronized List<Map<String, Object>> history() {
        ConfigurationSection history = data.getConfigurationSection("history");
        if (history == null) return List.of();
        List<Map<String, Object>> seasons = new ArrayList<>();
        for (String id : history.getKeys(false)) {
            ConfigurationSection season = history.getConfigurationSection(id);
            if (season == null) continue;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", id);
            entry.put("name", season.getString("name", "Season"));
            entry.put("started-at", season.getLong("started-at"));
            entry.put("ended-at", season.getLong("ended-at"));
            entry.put("ranking", List.copyOf(season.getMapList("ranking")));
            seasons.add(Collections.unmodifiableMap(entry));
        }
        seasons.sort(Comparator.comparingLong(entry -> -((Number) entry.get("ended-at")).longValue()));
        return List.copyOf(seasons);
    }
    private void save(){try{data.save(file);}catch(IOException e){plugin.getLogger().severe("Could not save seasons.yml: "+e.getMessage());}}
    public record SeasonResult(String id,String name,List<ClanTopEntry> ranking){}
}
