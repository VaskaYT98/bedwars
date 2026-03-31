package org.vaska.bedwars.spectator;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.vaska.bedwars.Bedwars;

/**
 * Simple spectator handler: sets flight, invisibility and disables interactions.
 */
public class SpectatorManager {
    private final Bedwars plugin;

    public SpectatorManager(Bedwars plugin) { this.plugin = plugin; }

    public void setSpectator(Player player) {
        if (Bukkit.isPrimaryThread()) {
            player.setGameMode(GameMode.SPECTATOR);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.sendMessage("&aYou are now a spectator");
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setGameMode(GameMode.SPECTATOR);
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage("&aYou are now a spectator");
            });
        }
    }

    public void removeSpectator(Player player) {
        if (Bukkit.isPrimaryThread()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage("&aYou left spectator mode");
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage("&aYou left spectator mode");
            });
        }
    }
}
