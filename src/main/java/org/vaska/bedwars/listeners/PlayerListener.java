package org.vaska.bedwars.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.vaska.bedwars.Bedwars;
import org.vaska.bedwars.db.PlayerDataManager;

import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final Bedwars plugin = Bedwars.getInstance();
    private final Map<UUID, java.util.Map.Entry<String, Long>> lastArena = new java.util.concurrent.ConcurrentHashMap<>();
    private final long rejoinTimeoutSeconds = Bedwars.getInstance().getConfig().getLong("rejoin_timeout_seconds", 300);

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        PlayerDataManager.PlayerStats stats = plugin.getPlayerDataManager().getCached(uuid);
        if (stats != null) plugin.getPlayerDataManager().saveStatsAsync(stats);
        plugin.getLogger().fine("Saved stats for " + uuid);
        // store last arena if any with timestamp for rejoin window
        plugin.getArenaManager().getArenaForPlayer(uuid).ifPresent(a -> lastArena.put(uuid, new java.util.AbstractMap.SimpleEntry<>(a.getId(), System.currentTimeMillis())));
        // persist to disk so rejoin survives server restarts
        try {
            java.io.File f = new java.io.File(plugin.getDataFolder(), "rejoin.yml");
            org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
            plugin.getArenaManager().getArenaForPlayer(uuid).ifPresent(a -> {
                cfg.set(uuid.toString() + ".arena", a.getId());
                cfg.set(uuid.toString() + ".ts", System.currentTimeMillis());
                try { cfg.save(f); } catch (Exception ex) { plugin.getLogger().warning("Failed to save rejoin.yml: " + ex.getMessage()); }
            });
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        java.util.Map.Entry<String, Long> entry = lastArena.remove(uuid);
        if (entry == null) {
            // try disk
            try {
                java.io.File f = new java.io.File(plugin.getDataFolder(), "rejoin.yml");
                if (f.exists()) {
                    org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
                    String aid = cfg.getString(uuid.toString() + ".arena", null);
                    long ts = cfg.getLong(uuid.toString() + ".ts", 0L);
                    if (aid != null) entry = new java.util.AbstractMap.SimpleEntry<>(aid, ts);
                }
            } catch (Throwable ignored) {}
        }
        if (entry != null) {
            long ts = entry.getValue();
            long now = System.currentTimeMillis();
            if ((now - ts) / 1000L <= rejoinTimeoutSeconds) {
                String arenaId = entry.getKey();
                plugin.getArenaManager().getArenaById(arenaId).ifPresent(a -> {
                    // attempt to rejoin (arena will handle capacity checks)
                    a.join(e.getPlayer());
                    e.getPlayer().sendMessage("&aRejoined arena " + arenaId);
                    // remove persisted entry
                    try {
                        java.io.File f = new java.io.File(plugin.getDataFolder(), "rejoin.yml");
                        if (f.exists()) {
                            org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
                            cfg.set(uuid.toString(), null);
                            cfg.set(uuid.toString() + ".arena", null);
                            cfg.set(uuid.toString() + ".ts", null);
                            cfg.save(f);
                        }
                    } catch (Throwable ignored) {}
                });
            } else {
                // expired
                plugin.getLogger().fine("Rejoin window expired for " + uuid);
            }
        }
    }
}
