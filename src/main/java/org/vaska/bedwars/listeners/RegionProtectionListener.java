package org.vaska.bedwars.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.vaska.bedwars.Bedwars;

import java.util.List;

/**
 * Prevents block interactions outside allowed regions for arenas.
 */
public class RegionProtectionListener implements Listener {
    private final Bedwars plugin = Bedwars.getInstance();

    private boolean isInProtectedRegion(Location loc) {
        return plugin.getArenaManager().isLocationProtected(loc);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (isInProtectedRegion(e.getBlock().getLocation())) {
            // allow if block type is whitelisted for interaction
            if (!Bedwars.getInstance().getArenaManager().isWhitelisted(e.getBlock().getType())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (isInProtectedRegion(e.getBlock().getLocation())) {
            if (!Bedwars.getInstance().getArenaManager().isWhitelisted(e.getBlock().getType())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() != null && isInProtectedRegion(e.getClickedBlock().getLocation())) {
            if (!Bedwars.getInstance().getArenaManager().isWhitelisted(e.getClickedBlock().getType())) {
                e.setCancelled(true);
            }
        }
    }
}
