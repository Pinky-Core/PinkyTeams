package me.pinkycore.pinkyteams.api.event;
import org.bukkit.entity.Player; import org.bukkit.event.Event; import org.bukkit.event.HandlerList;
public final class ClanLeftEvent extends Event {
 private static final HandlerList HANDLERS=new HandlerList(); private final Player player; private final String clanName; private final boolean clanDeleted;
 public ClanLeftEvent(Player player,String clanName,boolean clanDeleted){this.player=player;this.clanName=clanName;this.clanDeleted=clanDeleted;}
 public Player getPlayer(){return player;} public String getClanName(){return clanName;} public boolean wasClanDeleted(){return clanDeleted;}
 public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
