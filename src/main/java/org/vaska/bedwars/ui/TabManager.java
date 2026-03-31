package org.vaska.bedwars.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.vaska.bedwars.Bedwars;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Simple tab header/footer updater. Lightweight periodic updates using cached stats.
 */
public class TabManager {
    private final Bedwars plugin;
    private org.bukkit.scheduler.BukkitTask task;

    public TabManager(Bedwars plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.getPluginManager().getPlugin("TAB") != null) return;
                for (Player p : Bukkit.getOnlinePlayers()) updatePlayer(p);
            }
        }.runTaskTimer(plugin, 20L, 60L);
    }

    public void stop() {
        if (this.task != null) this.task.cancel();
    }

    public void updatePlayer(Player p) {
        org.vaska.bedwars.db.PlayerDataManager pdm = plugin.getPlayerDataManager();
        int wins = pdm != null ? pdm.getCachedValue(p.getUniqueId(), "wins") : 0;
        int kills = pdm != null ? pdm.getCachedValue(p.getUniqueId(), "kills") : 0;

        Component header = Component.text("BedWars - Map: ")
                .color(NamedTextColor.GOLD)
                .append(Component.text(getMapForPlayer(p)).color(NamedTextColor.WHITE));
        Component footer = Component.text("Wins: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(wins).color(NamedTextColor.WHITE))
                .append(Component.text("  Kills: ").color(NamedTextColor.GRAY))
                .append(Component.text(kills).color(NamedTextColor.WHITE));

        try {
            // Новый Adventure API
            p.sendPlayerListHeader(header);
            p.sendPlayerListFooter(footer);
        } catch (NoSuchMethodError e) {
            // fallback для старого API
            p.setPlayerListHeaderFooter(header.toString(), footer.toString());
        }
    }

    private String getMapForPlayer(Player p) {
        org.vaska.bedwars.arena.ArenaManager am = plugin.getArenaManager();
        if (am == null) return "lobby";
        java.util.Optional<org.vaska.bedwars.arena.Arena> a = am.getArenaForPlayer(p.getUniqueId());
        return a.map(org.vaska.bedwars.arena.Arena::getTemplateName).orElse("lobby");
    }
}