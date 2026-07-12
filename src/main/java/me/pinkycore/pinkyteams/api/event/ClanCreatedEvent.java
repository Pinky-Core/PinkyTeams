package me.pinkycore.pinkyteams.api.event;
import org.bukkit.entity.Player; import org.bukkit.event.Event; import org.bukkit.event.HandlerList;
public final class ClanCreatedEvent extends Event {
 private static final HandlerList HANDLERS=new HandlerList(); private final Player creator; private final String clanName;
 public ClanCreatedEvent(Player creator,String clanName){this.creator=creator;this.clanName=clanName;}
 public Player getCreator(){return creator;} public String getClanName(){return clanName;}
 public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
