package org.vaska.bedwars.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.vaska.bedwars.Bedwars;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Adapter to integrate with the TAB plugin if present. Uses reflection so we don't need a compile-time
 * dependency on TAB. If reflection fails, caller should fall back to built-in TabManager.
 */
public class TabAdapter {
    private final Bedwars plugin;
    private Object apiInstance;
    private Method getPlayerMethod;
    private Method setHeaderMethod;
    private Method setFooterMethod;
    private Method setHeaderFooterMethod;
    private org.bukkit.scheduler.BukkitTask task;
    private boolean available = false;

    public TabAdapter(Bedwars plugin) {
        this.plugin = plugin;
        try {
            Class<?> apiClass = Class.forName("me.neznamy.tab.api.TabAPI");
            Method getInst = apiClass.getMethod("getInstance");
            this.apiInstance = getInst.invoke(null);
            // find getPlayer(UUID) or getPlayer(String)
            try {
                this.getPlayerMethod = apiClass.getMethod("getPlayer", UUID.class);
            } catch (NoSuchMethodException e) {
                try { this.getPlayerMethod = apiClass.getMethod("getPlayer", String.class); } catch (NoSuchMethodException ignored) {}
            }
            // determine player class and header/footer setters
            Class<?> playerClass = null;
            if (this.getPlayerMethod != null) {
                Class<?>[] params = this.getPlayerMethod.getParameterTypes();
                // call to find returned player object later
                // attempt to load TabPlayer class
                try { playerClass = Class.forName("me.neznamy.tab.api.TabPlayer"); } catch (ClassNotFoundException ignored) {}
            }
            if (playerClass != null) {
                try { this.setHeaderMethod = playerClass.getMethod("setHeader", String.class); } catch (NoSuchMethodException ignored) {}
                try { this.setFooterMethod = playerClass.getMethod("setFooter", String.class); } catch (NoSuchMethodException ignored) {}
                try { this.setHeaderFooterMethod = playerClass.getMethod("setHeaderAndFooter", String.class, String.class); } catch (NoSuchMethodException ignored) {}
            }
            this.available = this.apiInstance != null && this.getPlayerMethod != null && (this.setHeaderFooterMethod != null || (this.setHeaderMethod != null && this.setFooterMethod != null));
            if (!this.available) plugin.getLogger().info("TAB detected but adapter reflection incomplete; falling back to built-in tab manager");
        } catch (Throwable t) {
            // TAB not present or reflection failed
            this.available = false;
        }
    }

    public boolean isAvailable() { return available; }

    public void start() {
        if (!available) return;
        this.task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) updatePlayer(p);
            }
        }.runTaskTimer(plugin, 20L, 60L);
        plugin.getLogger().info("TAB adapter started");
    }

    public void stop() {
        if (this.task != null) this.task.cancel();
    }

    private void updatePlayer(Player p) {
        try {
            Object tabPlayer = null;
            try {
                // try UUID
                tabPlayer = getPlayerMethod.invoke(apiInstance, p.getUniqueId());
            } catch (IllegalArgumentException iae) {
                try { tabPlayer = getPlayerMethod.invoke(apiInstance, p.getName()); } catch (IllegalArgumentException ignored) {}
            }
            if (tabPlayer == null) return;
            String header = "§6BedWars §7- §eMap: §f" + getMapForPlayer(p);
            org.vaska.bedwars.db.PlayerDataManager pdm = plugin.getPlayerDataManager();
            int wins = pdm != null ? pdm.getCachedValue(p.getUniqueId(), "wins") : -1;
            int kills = pdm != null ? pdm.getCachedValue(p.getUniqueId(), "kills") : -1;
            String footer = "§7Wins: §f" + (wins < 0 ? 0 : wins) + "  §7Kills: §f" + (kills < 0 ? 0 : kills);
            if (setHeaderFooterMethod != null) {
                setHeaderFooterMethod.invoke(tabPlayer, header, footer);
            } else {
                if (setHeaderMethod != null) setHeaderMethod.invoke(tabPlayer, header);
                if (setFooterMethod != null) setFooterMethod.invoke(tabPlayer, footer);
            }
        } catch (InvocationTargetException | IllegalAccessException ignored) {
            // ignore per-player failures
        } catch (Throwable t) {
            plugin.getLogger().warning("TAB adapter update failed: " + t.getMessage());
        }
    }

    private String getMapForPlayer(Player p) {
        org.vaska.bedwars.arena.ArenaManager am = plugin.getArenaManager();
        if (am == null) return "lobby";
        java.util.Optional<org.vaska.bedwars.arena.Arena> a = am.getArenaForPlayer(p.getUniqueId());
        return a.map(org.vaska.bedwars.arena.Arena::getTemplateName).orElse("lobby");
    }
}
