package me.pinkycore.pinkyteams.api.event;
import org.bukkit.entity.Player; import org.bukkit.event.Event; import org.bukkit.event.HandlerList;
public final class ClanDisbandedEvent extends Event {
 private static final HandlerList HANDLERS=new HandlerList(); private final Player actor; private final String clanName;
 public ClanDisbandedEvent(Player actor,String clanName){this.actor=actor;this.clanName=clanName;}
 public Player getActor(){return actor;} public String getClanName(){return clanName;}
 public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
