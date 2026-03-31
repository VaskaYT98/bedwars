package org.vaska.bedwars.protect;

import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.vaska.bedwars.Bedwars;

/**
 * Removes or purges purchased items from the world when chunks/worlds unload to avoid duplication exploits.
 */
public class UnloadProtectionListener implements Listener {
    private final Bedwars plugin;
    private final NamespacedKey boughtKey;

    public UnloadProtectionListener(Bedwars plugin) {
        this.plugin = plugin;
        this.boughtKey = new NamespacedKey(plugin, "bw_purchased");
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        try {
            for (org.bukkit.entity.Entity ent : e.getChunk().getEntities()) {
                if (ent instanceof org.bukkit.entity.Item) {
                    org.bukkit.entity.Item item = (org.bukkit.entity.Item) ent;
                    if (item.getItemStack() == null || item.getItemStack().getItemMeta() == null) continue;
                    try {
                        if (item.getItemStack().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) ||
                                item.getItemStack().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE)) {
                            item.remove();
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        try {
            World w = e.getWorld();
            for (org.bukkit.entity.Entity ent : w.getEntities()) {
                if (ent instanceof org.bukkit.entity.Item) {
                    org.bukkit.entity.Item item = (org.bukkit.entity.Item) ent;
                    if (item.getItemStack() == null || item.getItemStack().getItemMeta() == null) continue;
                    try {
                        if (item.getItemStack().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) ||
                                item.getItemStack().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE)) {
                            item.remove();
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }
}
