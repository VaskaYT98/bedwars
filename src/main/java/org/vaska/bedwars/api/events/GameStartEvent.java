package org.vaska.bedwars.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String arenaId;

    public GameStartEvent(String arenaId) { this.arenaId = arenaId; }
    public String getArenaId() { return arenaId; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
