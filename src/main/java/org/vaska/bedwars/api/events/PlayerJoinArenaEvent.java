package org.vaska.bedwars.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerJoinArenaEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final String arenaId;

    public PlayerJoinArenaEvent(Player player, String arenaId) { this.player = player; this.arenaId = arenaId; }

    public Player getPlayer() { return player; }
    public String getArenaId() { return arenaId; }

    @Override public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
