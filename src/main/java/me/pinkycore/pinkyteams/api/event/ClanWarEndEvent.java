package me.pinkycore.pinkyteams.api.event;
import me.pinkycore.pinkyteams.service.ClanWarService; import org.bukkit.event.Event; import org.bukkit.event.HandlerList;
public final class ClanWarEndEvent extends Event {
 private static final HandlerList HANDLERS=new HandlerList(); private final ClanWarService.WarResult result;
 public ClanWarEndEvent(ClanWarService.WarResult result){this.result=result;} public ClanWarService.WarResult getResult(){return result;}
 public HandlerList getHandlers(){return HANDLERS;} public static HandlerList getHandlerList(){return HANDLERS;}
}
