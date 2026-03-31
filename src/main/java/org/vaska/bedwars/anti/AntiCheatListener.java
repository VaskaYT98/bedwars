package org.vaska.bedwars.anti;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;

/**
 * Lightweight reach check: cancels damage if attacker is too far.
 */
public class AntiCheatListener implements Listener {

    private static final double MAX_REACH_SQ = 6.0 * 6.0; // 6 blocks

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        if (!(e.getEntity() instanceof Player)) return;
        Player attacker = (Player) e.getDamager();
        Player victim = (Player) e.getEntity();
        double distSq = attacker.getLocation().distanceSquared(victim.getLocation());
        if (distSq > MAX_REACH_SQ) {
            e.setCancelled(true);
            attacker.sendMessage("&cReach detected, action cancelled.");
        }
    }
}
