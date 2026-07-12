package me.pinkycore.pinkyteams.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class ClanBankTransactionEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player; private final String clanName; private final double amount;
    private final double balance; private final Type type;
    public ClanBankTransactionEvent(Player player, String clanName, double amount, double balance, Type type) {
        this.player = player; this.clanName = clanName; this.amount = amount; this.balance = balance; this.type = type;
    }
    public Player getPlayer() { return player; }
    public String getClanName() { return clanName; }
    public double getAmount() { return amount; }
    public double getBalance() { return balance; }
    public Type getType() { return type; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
    public enum Type { DEPOSIT, WITHDRAW }
}
