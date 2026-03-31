package org.vaska.bedwars.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BedBreakEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player breaker;
    private final String arenaId;

    public BedBreakEvent(Player breaker, String arenaId) { this.breaker = breaker; this.arenaId = arenaId; }
    public Player getBreaker() { return breaker; }
    public String getArenaId() { return arenaId; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
