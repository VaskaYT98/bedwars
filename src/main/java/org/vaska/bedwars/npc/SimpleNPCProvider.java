package org.vaska.bedwars.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builtin fallback NPC provider using invisible ArmorStands with custom name and metadata.
 */
public class SimpleNPCProvider implements NPCProvider {
    private final JavaPlugin plugin;
    private final Map<Integer, String> registry = new LinkedHashMap<>();
    private int nextId = 1;

    public SimpleNPCProvider(JavaPlugin plugin) { this.plugin = plugin; }

    @Override
    public void initialize() {
        // register click listener
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent e) {
                if (e.getRightClicked() instanceof ArmorStand) {
                    ArmorStand a = (ArmorStand) e.getRightClicked();
                    if (a.getCustomName() != null && a.getCustomName().startsWith("BW_NPC:")) {
                        // simple action: run join
                        e.getPlayer().performCommand("bw join");
                    }
                }
            }
        }, plugin);
    }

    @Override
    public boolean isAvailable() { return true; }

    @Override
    public int createNPC(String templateName, String npcName, Location loc) {
        ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        as.setCustomNameVisible(true);
        as.setCustomName("BW_NPC:" + npcName);
        as.setGravity(false);
        as.setInvulnerable(true);
        as.setPersistent(true);
        int id = nextId++;
        registry.put(id, npcName);
        return id;
    }

    @Override
    public boolean removeNPC(int id) {
        // best-effort: look for armor stand with name
        String name = registry.remove(id);
        if (name == null) return false;
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity ent : w.getEntities()) {
                if (ent instanceof ArmorStand) {
                    ArmorStand a = (ArmorStand) ent;
                    if (("BW_NPC:" + name).equals(a.getCustomName())) {
                        a.remove();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Map<Integer, String> listNPCs() { return java.util.Collections.unmodifiableMap(registry); }
}
