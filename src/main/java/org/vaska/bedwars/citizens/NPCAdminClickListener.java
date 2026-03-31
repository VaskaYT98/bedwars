package org.vaska.bedwars.citizens;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.vaska.bedwars.Bedwars;

public class NPCAdminClickListener implements Listener {
    private final Bedwars plugin = Bedwars.getInstance();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        try {
            if (e.getView() == null) return;
            String title = e.getView().getTitle();
            if (title == null || !title.equals("BedWars NPCs")) return;
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta().getDisplayName() == null) return;
            String name = clicked.getItemMeta().getDisplayName();
            // expecting format: DisplayName (#id)
            int idx = name.lastIndexOf("(#");
            if (idx < 0) return;
            int end = name.indexOf(')', idx);
            if (end < 0) return;
            String num = name.substring(idx + 2, end);
            int id;
            try { id = Integer.parseInt(num); } catch (NumberFormatException ex) { return; }
            org.vaska.bedwars.citizens.CitizensManager cm = plugin.getCitizensManager();
            if (cm == null || !cm.isEnabled()) { e.getWhoClicked().sendMessage("Citizens not available"); return; }
            boolean ok = cm.removeNPC(id);
            e.getWhoClicked().sendMessage(ok ? "NPC removed (id=" + id + ")" : "Failed to remove NPC");
            e.getWhoClicked().closeInventory();
        } catch (Throwable ignored) {}
    }
}
