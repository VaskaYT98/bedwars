package org.vaska.bedwars.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.vaska.bedwars.Bedwars;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple periodic scoreboard manager. Uses cached player stats for light updates.
 */
public class ScoreboardManager {
    private final Bedwars plugin;
    private org.bukkit.scheduler.BukkitTask task;
    private final Map<UUID, Scoreboard> boards = new ConcurrentHashMap<>();

    public ScoreboardManager(Bedwars plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // run every 2 seconds (40 ticks)
        this.task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) updatePlayer(p);
            }
        }.runTaskTimer(plugin, 20L, 40L);
    }

    public void stop() {
        if (this.task != null) this.task.cancel();
        this.boards.clear();
    }

    public void updatePlayer(Player p) {
        org.bukkit.scoreboard.ScoreboardManager mgr = Bukkit.getScoreboardManager(); // полностью квалифицированный класс
        if (mgr == null) return;

        // создаём или достаём доску игрока
        Scoreboard board = boards.computeIfAbsent(p.getUniqueId(), k -> mgr.getNewScoreboard());

        Objective obj = board.getObjective("bw_sidebar");
        if (obj == null) obj = board.registerNewObjective("bw_sidebar", "dummy", "BedWars");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // populate a few lines
        org.vaska.bedwars.db.PlayerDataManager pdm = plugin.getPlayerDataManager();
        int wins = pdm != null ? pdm.getCachedValue(p.getUniqueId(), "wins") : 0;
        int kills = pdm != null ? pdm.getCachedValue(p.getUniqueId(), "kills") : 0;

        obj.getScore("Wins: " + wins).setScore(3);
        obj.getScore("Kills: " + kills).setScore(2);
        obj.getScore("Map: " + getMapForPlayer(p)).setScore(1);

        p.setScoreboard(board);
    }

    private String getMapForPlayer(Player p) {
        org.vaska.bedwars.arena.ArenaManager am = plugin.getArenaManager();
        if (am == null) return "lobby";
        java.util.Optional<org.vaska.bedwars.arena.Arena> a = am.getArenaForPlayer(p.getUniqueId());
        return a.map(org.vaska.bedwars.arena.Arena::getTemplateName).orElse("lobby");
    }
}
