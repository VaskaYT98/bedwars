package org.vaska.bedwars.citizens;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.vaska.bedwars.Bedwars;

import java.util.Map;

public class NPCAdminGUI {
    private final Bedwars plugin = Bedwars.getInstance();
    private final CitizensManager cm;

    public NPCAdminGUI(CitizensManager cm) {
        this.cm = cm;
    }

    public void open(Player p) {
        Map<Integer, String> list = cm.listNPCs();
        int size = Math.max(9, ((list.size() + 8) / 9) * 9);
        Inventory inv = Bukkit.createInventory(null, size, "BedWars NPCs");
        int slot = 0;
        for (Map.Entry<Integer, String> e : list.entrySet()) {
            ItemStack it = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName(e.getValue() + " (#" + e.getKey() + ")");
            it.setItemMeta(m);
            inv.setItem(slot++, it);
        }
        p.openInventory(inv);
        // click handling is routed by a generic shop listener already present; admin can remove via /bw npc remove <id>
    }
}
