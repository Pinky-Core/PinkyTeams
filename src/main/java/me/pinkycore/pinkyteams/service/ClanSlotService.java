package me.pinkycore.pinkyteams.service;
import me.pinkycore.pinkyteams.PinkyTeams; import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
public final class ClanSlotService{
 private final PinkyTeams plugin; public ClanSlotService(PinkyTeams plugin){this.plugin=plugin;}
 private final java.util.concurrent.ConcurrentHashMap<String,Object> locks=new java.util.concurrent.ConcurrentHashMap<>();
 public boolean enabled(){return plugin.getConfig().getBoolean("clan-slots.enabled",false);}
 public boolean usesPoints(){return plugin.getConfig().getBoolean("clan-slots.use-points",true);}
 public int memberCount(String clan){return plugin.getStorageProvider().getClanMemberCount(clan);}
 public int limit(String clan){
  if(!enabled())return Integer.MAX_VALUE;if(!usesPoints()){int value=plugin.getConfig().getInt("clan-slots.static-limit",0);return value<=0?Integer.MAX_VALUE:value;}
  long total=Math.max(0,plugin.getConfig().getInt("clan-slots.base-slots",0));int bought=plugin.getStorageProvider().getClanSlotUpgrades(clan);
  List<Upgrade> upgrades=upgrades();for(int i=0;i<Math.min(bought,upgrades.size());i++)total+=upgrades.get(i).slots;
  return total>=Integer.MAX_VALUE?Integer.MAX_VALUE:(int)total;
 }
 public boolean hasSpace(String clan){return limit(clan)==Integer.MAX_VALUE||memberCount(clan)<limit(clan);}
 public Status status(String clan){int bought=plugin.getStorageProvider().getClanSlotUpgrades(clan),points=plugin.getStorageProvider().getClanPoints(clan);
  List<Upgrade> all=upgrades();return new Status(memberCount(clan),limit(clan),points,bought,bought<all.size()?all.get(bought):null);}
 public Purchase buy(String clan){synchronized(locks.computeIfAbsent(clan.toLowerCase(Locale.ROOT),ignored->new Object())){Status s=status(clan);if(s.next==null)return Purchase.NO_MORE;
  if(s.points<s.next.cost)return Purchase.NOT_ENOUGH;plugin.getStorageProvider().setClanPoints(clan,s.points-s.next.cost);
  plugin.getStorageProvider().setClanSlotUpgrades(clan,s.purchased+1);return new Purchase(Result.SUCCESS,s.next,limit(clan));}}
 private List<Upgrade> upgrades(){List<Upgrade> out=new ArrayList<>();for(Map<?,?> row:plugin.getConfig().getMapList("clan-slots.upgrades")){
  int cost=number(row.get("cost")),slots=number(row.get("slots"));if(cost>=0&&slots>0)out.add(new Upgrade(cost,slots));}return out;}
 private int number(Object value){return value instanceof Number n?n.intValue():0;}
 public static String formatLimit(int limit){return limit==Integer.MAX_VALUE?"∞":String.valueOf(limit);}
 public enum Result{SUCCESS,NOT_ENOUGH,NO_MORE} public record Upgrade(int cost,int slots){}
 public record Status(int used,int limit,int points,int purchased,Upgrade next){}
 public record Purchase(Result result,Upgrade upgrade,int newLimit){static final Purchase NOT_ENOUGH=new Purchase(Result.NOT_ENOUGH,null,0),NO_MORE=new Purchase(Result.NO_MORE,null,0);}
}
