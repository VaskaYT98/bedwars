package org.vaska.bedwars.shop;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Handles shop GUI clicks and delegates to ShopGUI implementation.
 */
public class ShopListener implements Listener {
    private final ShopGUI shop = new ShopGUI();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getView().getTitle().equals("BedWars Shop")) shop.onClick(e);
    }

    // open shop command hooks not shown here; GUI is opened by commands elsewhere
}

