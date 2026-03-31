package org.vaska.bedwars.anti;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataType;
import org.vaska.bedwars.Bedwars;

/**
 * Prevents trading of PDC-marked purchased items between players (trade window and merchant).
 */
public class TradeProtectionListener implements Listener {
    private final NamespacedKey boughtKey;

    public TradeProtectionListener(Bedwars plugin) {
        this.boughtKey = new NamespacedKey(plugin, "bw_purchased");
    }

    @EventHandler
    public void onTradeClick(InventoryClickEvent e) {
        try {
            if (e.getInventory() == null) return;
            InventoryType invType = e.getInventory().getType();
            if (invType != InventoryType.MERCHANT && invType != InventoryType.CRAFTING && invType != InventoryType.CHEST) {
                // only care about merchant/trade inventories
            }
            // if either cursor or current has PDC, and the clicked inventory is not the player's own, block
            if (e.getCurrentItem() != null && e.getCurrentItem().hasItemMeta()) {
                if (e.getCurrentItem().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) ||
                        e.getCurrentItem().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE)) {
                    e.setCancelled(true);
                    try { Bedwars.getInstance().getLogger().info("Blocked trade click: current item marked purchased"); } catch (Throwable ignored) {}
                    return;
                }
            }
            if (e.getCursor() != null && e.getCursor().hasItemMeta()) {
                if (e.getCursor().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) ||
                        e.getCursor().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE)) {
                    e.setCancelled(true);
                    try { Bedwars.getInstance().getLogger().info("Blocked trade click: cursor item marked purchased"); } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }
}
