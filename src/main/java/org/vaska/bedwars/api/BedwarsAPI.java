package org.vaska.bedwars.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.vaska.bedwars.Bedwars;
import org.vaska.bedwars.arena.Arena;
import org.vaska.bedwars.db.PlayerDataManager;
import org.vaska.bedwars.party.Party;

import java.util.Optional;

public final class BedwarsAPI {
    private static final Bedwars plugin = Bedwars.getInstance();

    public static Optional<Arena> getArena(Player player) {
        return plugin.getArenaManager().getArenaForPlayer(player.getUniqueId());
    }

    public static Optional<PlayerDataManager.PlayerStats> getStats(OfflinePlayer player) {
        if (plugin.getPlayerDataManager() == null) return Optional.empty();
        return Optional.ofNullable(plugin.getPlayerDataManager().getCached(player.getUniqueId()));
    }

    public static Optional<Party> getParty(Player player) {
        return plugin.getPartyManager().getParty(player.getUniqueId());
    }
}
