package org.vaska.bedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.vaska.bedwars.Bedwars;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player activity and marks players AFK after a timeout.
 */
public class AFKManager implements Listener {
    private final Bedwars plugin;
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final long timeoutSeconds;

    public AFKManager(Bedwars plugin, long timeoutSeconds) {
        this.plugin = plugin; this.timeoutSeconds = timeoutSeconds;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // schedule checker
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkAFK, 20L*30, 20L*30);
    }

    private void checkAFK() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            long last = lastActivity.getOrDefault(p.getUniqueId(), now);
            if ((now - last) / 1000L >= timeoutSeconds) {
                // schedule any player-state changes on the main server thread to avoid sync/async errors
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        // move to spectator if in a game
                        plugin.getSpectatorManager().setSpectator(p);
                        p.sendMessage("&cYou were moved to spectator due to inactivity.");
                    } catch (Throwable t) {
                        plugin.getLogger().warning("AFKManager: failed to set spectator for " + p.getName() + ": " + t.getMessage());
                    }
                });
            }
        }
    }

    private void touch(Player p) { lastActivity.put(p.getUniqueId(), System.currentTimeMillis()); }

    @EventHandler
    public void onMove(PlayerMoveEvent e) { touch(e.getPlayer()); }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) { touch(e.getPlayer()); }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) { touch(e.getPlayer()); }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) { touch(e.getPlayer()); }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { touch(e.getPlayer()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { lastActivity.remove(e.getPlayer().getUniqueId()); }
}
