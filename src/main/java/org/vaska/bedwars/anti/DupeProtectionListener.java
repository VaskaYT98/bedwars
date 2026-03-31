package org.vaska.bedwars.anti;

import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.vaska.bedwars.Bedwars;

public class DupeProtectionListener implements Listener {
    private final Bedwars plugin;
    private final NamespacedKey boughtKey;

    public DupeProtectionListener(Bedwars plugin) {
        this.plugin = plugin;
        this.boughtKey = new NamespacedKey(plugin, "bw_purchased");
    }

    @EventHandler
    public void onInventoryMove(org.bukkit.event.inventory.InventoryMoveItemEvent e) {
        try {
            if (e.getItem() == null || e.getItem().getItemMeta() == null) return;
            if (e.getItem().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) ||
                    e.getItem().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE)) {
                e.setCancelled(true);
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onInventoryPickup(org.bukkit.event.inventory.InventoryPickupItemEvent e) {
        try {
            if (e.getItem() == null || e.getItem().getItemStack() == null || e.getItem().getItemStack().getItemMeta() == null) return;
            if (e.getItem().getItemStack().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) ||
                    e.getItem().getItemStack().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE)) {
                e.setCancelled(true);
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onEntityPickup(org.bukkit.event.entity.EntityPickupItemEvent e) {
        try {
            if (e.getItem() == null || e.getItem().getItemStack() == null || e.getItem().getItemStack().getItemMeta() == null) return;
            if (e.getItem().getItemStack().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) ||
                    e.getItem().getItemStack().getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE)) {
                e.setCancelled(true);
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        try {
            for (org.bukkit.inventory.ItemStack is : e.getNewItems().values()) {
                if (is == null || !is.hasItemMeta()) continue;
                if (is.getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) ||
                        is.getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE)) {
                    e.setCancelled(true);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onPrepareCraft(org.bukkit.event.inventory.PrepareItemCraftEvent e) {
        try {
            org.bukkit.inventory.CraftingInventory inv = e.getInventory();
            org.bukkit.inventory.ItemStack result = e.getInventory().getResult();
            if (result == null) return;
            for (org.bukkit.inventory.ItemStack mat : inv.getMatrix()) {
                if (mat == null || !mat.hasItemMeta()) continue;
                if (mat.getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) ||
                        mat.getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE)) {
                    inv.setResult(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onInventoryClickGeneral(org.bukkit.event.inventory.InventoryClickEvent e) {
        try {
            if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player)) return;
            org.bukkit.entity.Player p = (org.bukkit.entity.Player) e.getWhoClicked();
            org.bukkit.inventory.ItemStack current = e.getCurrentItem();
            org.bukkit.inventory.ItemStack cursor = e.getCursor();
            // if trying to move a purchased item into another inventory (shops/hoppers/other players), block
            if (e.getClickedInventory() != null && e.getClickedInventory() != p.getInventory()) {
                if (current != null && current.hasItemMeta() && (current.getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) || current.getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE))) {
                    e.setCancelled(true);
                    return;
                }
                if (cursor != null && cursor.hasItemMeta() && (cursor.getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.STRING) || cursor.getItemMeta().getPersistentDataContainer().has(boughtKey, PersistentDataType.BYTE))) {
                    e.setCancelled(true);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onItemMerge(ItemMergeEvent e) {
        if (e.getEntity() == null || e.getTarget() == null) return;
        PersistentDataContainer a = e.getEntity().getItemStack().getItemMeta() != null
                ? e.getEntity().getItemStack().getItemMeta().getPersistentDataContainer() : null;
        PersistentDataContainer b = e.getTarget().getItemStack().getItemMeta() != null
                ? e.getTarget().getItemStack().getItemMeta().getPersistentDataContainer() : null;
        try {
            boolean aHas = a != null && (a.has(boughtKey, PersistentDataType.BYTE) || a.has(boughtKey, PersistentDataType.STRING));
            boolean bHas = b != null && (b.has(boughtKey, PersistentDataType.BYTE) || b.has(boughtKey, PersistentDataType.STRING));
            if (aHas || bHas) e.setCancelled(true);
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        try {
            if (e.isShiftClick()) {
                if (e.getCurrentItem() != null && e.getCurrentItem().getItemMeta() != null) {
                    PersistentDataContainer c = e.getCurrentItem().getItemMeta().getPersistentDataContainer();
                    if (c.has(boughtKey, PersistentDataType.BYTE) || c.has(boughtKey, PersistentDataType.STRING)) {
                        e.setCancelled(true);
                        return;
                    }
                }
                if (e.getCursor() != null && e.getCursor().getItemMeta() != null) {
                    PersistentDataContainer c2 = e.getCursor().getItemMeta().getPersistentDataContainer();
                    if (c2.has(boughtKey, PersistentDataType.BYTE) || c2.has(boughtKey, PersistentDataType.STRING)) {
                        e.setCancelled(true);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        try {
            if (e.getItemDrop() != null && e.getItemDrop().getItemStack() != null && e.getItemDrop().getItemStack().getItemMeta() != null) {
                PersistentDataContainer c = e.getItemDrop().getItemStack().getItemMeta().getPersistentDataContainer();
                if (c.has(boughtKey, PersistentDataType.BYTE) || c.has(boughtKey, PersistentDataType.STRING)) {
                    // prevent dropping purchased items to avoid exploit contexts
                    e.setCancelled(true);
                }
            }
        } catch (Throwable ignored) {}
    }
}
