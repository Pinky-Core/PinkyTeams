package me.pinkycore.pinkyteams.api.event;
import org.bukkit.entity.Player; import org.bukkit.event.Event; import org.bukkit.event.HandlerList;
public final class ClanJoinedEvent extends Event {
 private static final HandlerList HANDLERS=new HandlerList(); private final Player player; private final String clanName;
 public ClanJoinedEvent(Player player,String clanName){this.player=player;this.clanName=clanName;}
 public Player getPlayer(){return player;} public String getClanName(){return clanName;}
 public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
