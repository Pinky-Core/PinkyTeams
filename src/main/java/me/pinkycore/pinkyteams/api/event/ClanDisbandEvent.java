package me.pinkycore.pinkyteams.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class ClanDisbandEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player; private final String clanName; private boolean cancelled;
    public ClanDisbandEvent(Player player, String clanName) { this.player = player; this.clanName = clanName; }
    public Player getPlayer() { return player; }
    public String getClanName() { return clanName; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
