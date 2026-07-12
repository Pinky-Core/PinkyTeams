package me.pinkycore.pinkyteams.api.event;
import org.bukkit.entity.Player; import org.bukkit.event.Event; import org.bukkit.event.HandlerList;
public final class ClanAllianceCreatedEvent extends Event {
 private static final HandlerList HANDLERS=new HandlerList(); private final Player actor; private final String firstClan; private final String secondClan;
 public ClanAllianceCreatedEvent(Player actor,String firstClan,String secondClan){this.actor=actor;this.firstClan=firstClan;this.secondClan=secondClan;}
 public Player getActor(){return actor;} public String getFirstClan(){return firstClan;} public String getSecondClan(){return secondClan;}
 public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
