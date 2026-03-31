package org.vaska.bedwars.arena;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.vaska.bedwars.Bedwars;

import java.util.List;
import java.util.Map;

/**
 * Simple Inventory-based editor UI showing checklist and action buttons.
 */
public class ArenaEditorGUI implements Listener {
    private final Bedwars plugin;
    private final ArenaEditor editor;
    private final String title = ChatColor.DARK_AQUA + "BedWars Arena Editor";

    public ArenaEditorGUI(Bedwars plugin, ArenaEditor editor) {
        this.plugin = plugin; this.editor = editor;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player p) {
        Map<String, Boolean[]> teams = editor.getTeamsStatus(p);
        int rows = Math.max(3, (teams.size() + 4) / 9 + 1);
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);
        // fill team checklist with friendly colored wool and names
        int slot = 0;
        for (Map.Entry<String, Boolean[]> e : teams.entrySet()) {
            String name = e.getKey();
            Boolean[] b = e.getValue();
            Material woolMat = materialForTeamName(name);
            ItemStack wool = new ItemStack(woolMat);
            ItemMeta m = wool.getItemMeta();
            m.setDisplayName(colorForTeamName(name) + name);
            List<String> lore = new java.util.ArrayList<>();
            lore.add((b[0] ? ChatColor.GRAY : ChatColor.RED) + "Spawn: " + (b[0] ? "SET" : "MISSING"));
            lore.add((b[1] ? ChatColor.GRAY : ChatColor.RED) + "Bed: " + (b[1] ? "SET" : "MISSING"));
            m.setLore(lore);
            m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            wool.setItemMeta(m);
            inv.setItem(slot++, wool);
        }
        // actions: set pos1, set pos2, set spawn (uses selected team slot), set bed, save, cancel
        inv.setItem(rows*9 - 9 + 3, makeItem(Material.OAK_SIGN, ChatColor.YELLOW + "Set Pos1", "Set play_area position 1 to your current location"));
        inv.setItem(rows*9 - 9 + 4, makeItem(Material.OAK_SIGN, ChatColor.YELLOW + "Set Pos2", "Set play_area position 2 to your current location"));
        inv.setItem(rows*9 - 9 + 2, makeItem(Material.ENDER_PEARL, ChatColor.AQUA + "Set Selected Team Spawn", "Set spawn for the team you click on"));
        inv.setItem(rows*9 - 9 + 5, makeItem(Material.BEDROCK, ChatColor.AQUA + "Set Selected Team Bed", "Set bed location for the team you click on"));
        inv.setItem(rows*9 - 1, makeItem(Material.LIME_DYE, ChatColor.GREEN + "Save Template", "Validate and save the template"));
        inv.setItem(rows*9 - 7, makeItem(Material.RED_DYE, ChatColor.RED + "Cancel Edit", "Discard changes and exit editor"));
        p.openInventory(inv);
    }

    private ChatColor colorForTeamName(String name) {
        switch (name.toLowerCase()) {
            case "red": return ChatColor.RED;
            case "blue": return ChatColor.BLUE;
            case "green": return ChatColor.DARK_GREEN;
            case "yellow": return ChatColor.YELLOW;
            case "aqua": return ChatColor.AQUA;
            case "purple": return ChatColor.LIGHT_PURPLE;
            case "orange": return ChatColor.GOLD;
            case "white": return ChatColor.WHITE;
            case "black": return ChatColor.DARK_GRAY;
            case "brown": return ChatColor.GOLD;
            case "pink": return ChatColor.LIGHT_PURPLE;
            case "lime": return ChatColor.GREEN;
            default: return ChatColor.GRAY;
        }
    }

    private Material materialForTeamName(String name) {
        switch (name.toLowerCase()) {
            case "red": return Material.RED_WOOL;
            case "blue": return Material.BLUE_WOOL;
            case "green": return Material.GREEN_WOOL;
            case "yellow": return Material.YELLOW_WOOL;
            case "aqua": return Material.CYAN_WOOL;
            case "purple": return Material.PURPLE_WOOL;
            case "orange": return Material.ORANGE_WOOL;
            case "white": return Material.WHITE_WOOL;
            case "black": return Material.BLACK_WOOL;
            case "brown": return Material.BROWN_WOOL;
            case "pink": return Material.PINK_WOOL;
            case "lime": return Material.LIME_WOOL;
            default:
                // status color fallback
                return Material.GRAY_WOOL;
        }
    }

    private ItemStack makeItem(Material mat, String name, String desc) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(name);
        m.setLore(java.util.Collections.singletonList(ChatColor.GRAY + desc));
        it.setItemMeta(m);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!e.getView().getTitle().equals(title)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        int raw = e.getRawSlot();
        int size = e.getInventory().getSize();
        // identify clicks
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        String name = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : null;
        if (name == null) return;
        if (name.contains("Set Pos1")) {
            editor.setPos1(p);
            p.sendMessage("[BW] play_area pos1 set.");
            p.closeInventory();
            return;
        }
        if (name.contains("Set Pos2")) {
            editor.setPos2(p);
            p.sendMessage("[BW] play_area pos2 set.");
            p.closeInventory();
            return;
        }
        if (name.contains("Set Selected Team Spawn")) {
            // expect player to click a team slot afterwards; store metadata on player
            p.closeInventory();
            p.sendMessage("[BW] Now click a team item in the editor to set its spawn. Re-open the editor when done.");
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // open a special selection inventory showing teams again and mark mode
                Inventory inv = Bukkit.createInventory(null, 9, title + " - Select Team (Spawn)");
                Map<String, Boolean[]> teams = editor.getTeamsStatus(p);
                int slot = 0;
                for (Map.Entry<String, Boolean[]> e2 : teams.entrySet()) {
                    ItemStack it = new ItemStack(Material.PAPER);
                    ItemMeta m = it.getItemMeta();
                    m.setDisplayName(ChatColor.AQUA + e2.getKey());
                    it.setItemMeta(m);
                    inv.setItem(slot++, it);
                }
                p.openInventory(inv);
            });
            return;
        }
        if (name.contains("Set Selected Team Bed")) {
            p.closeInventory();
            p.sendMessage("[BW] Now click a team item in the editor to set its bed. Re-open the editor when done.");
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, 9, title + " - Select Team (Bed)");
                Map<String, Boolean[]> teams = editor.getTeamsStatus(p);
                int slot = 0;
                for (Map.Entry<String, Boolean[]> e2 : teams.entrySet()) {
                    ItemStack it = new ItemStack(Material.PAPER);
                    ItemMeta m = it.getItemMeta();
                    m.setDisplayName(ChatColor.AQUA + e2.getKey());
                    it.setItemMeta(m);
                    inv.setItem(slot++, it);
                }
                p.openInventory(inv);
            });
            return;
        }
        if (name.contains("Save Template")) {
            // validate
            java.util.List<String> missing = editor.validateSessionFor(p);
            if (!missing.isEmpty()) {
                p.sendMessage(ChatColor.RED + "Cannot save: missing items:");
                missing.forEach(s -> p.sendMessage(ChatColor.YELLOW + " - " + s));
                return;
            }
            boolean ok = editor.stopSessionSave(p);
            if (ok) p.sendMessage(ChatColor.GREEN + "Template saved.");
            p.closeInventory();
            return;
        }
        if (name.contains("Cancel Edit")) {
            editor.stopSessionCancel(p);
            p.closeInventory();
            return;
        }

        // handle team selection in selection inventories
        if (e.getView().getTitle().startsWith(title + " - Select Team (Spawn)")) {
            if (clicked.getItemMeta() != null && clicked.getItemMeta().getDisplayName() != null) {
                String team = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                editor.setTeamSpawn(p, team);
                p.sendMessage("[BW] Team " + team + " spawn set to your location.");
            }
            p.closeInventory();
            // reopen main editor
            open(p);
            return;
        }
        if (e.getView().getTitle().startsWith(title + " - Select Team (Bed)")) {
            if (clicked.getItemMeta() != null && clicked.getItemMeta().getDisplayName() != null) {
                String team = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                editor.setTeamBed(p, team);
                p.sendMessage("[BW] Team " + team + " bed set to your location.");
            }
            p.closeInventory();
            open(p);
            return;
        }
    }
}
