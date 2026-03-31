package org.vaska.bedwars.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.vaska.bedwars.Bedwars;

import java.util.*;

/** BedWars1058-style shop GUI: categories, paging, quick-buy, upgrades and trap queue. */
public class ShopGUI {
    private final ShopManager shopManager = new ShopManager(Bedwars.getInstance());
    private final UpgradeManager upgradeManager = new UpgradeManager(Bedwars.getInstance());
    private final ShopService shopService = new ShopService(Bedwars.getInstance(), shopManager);

    public ShopGUI() {
        shopManager.load();
        upgradeManager.reloadFromShop(shopManager);
    }

    public void open(Player player) {
        // build a simple single-page category view for now using menu-content
        ShopManager sm = shopManager;
        List<String> menu = sm.getUpgradeConfig().menuContent;
        int size = Math.max(9, sm.getUpgradeConfig().menuSize);
        Inventory inv = Bukkit.createInventory(null, size, "BedWars Shop");

        int slot = 0;
        for (String entry : menu) {
            if (entry == null) continue;
            String key = entry.split(",")[0].trim();
            // try to resolve upgrade or category
            if (sm.getUpgradeDefinitions().containsKey(key)) {
                UpgradeDefinition def = sm.getUpgradeDefinitions().get(key);
                // show first tier display
                UpgradeTier t = def.getTiers().values().stream().findFirst().orElse(null);
                ItemStack it = new ItemStack(Material.valueOf(t.displayMaterial));
                ItemMeta m = it.getItemMeta();
                m.setDisplayName(key);
                List<String> lore = new ArrayList<>();
                lore.add("Upgrade: " + key);
                lore.add("Cost: " + t.cost + " " + t.currency);
                m.setLore(lore);
                it.setItemMeta(m);
                inv.setItem(slot, it);
            } else {
                // fallback: place a separator glass or named icon if available
                if (key.startsWith("separator") || key.startsWith("category")) {
                    ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                    ItemMeta m = glass.getItemMeta(); m.setDisplayName(" "); glass.setItemMeta(m);
                    inv.setItem(slot, glass);
                }
            }
            slot++;
            if (slot >= size) break;
        }

        // quick-buy slot (example)
        int qbSlot = Math.min(8, size - 1);
        ItemStack qb = new ItemStack(Material.NETHER_STAR);
        ItemMeta qbm = qb.getItemMeta(); qbm.setDisplayName("Quick Buy"); qb.setItemMeta(qbm);
        inv.setItem(qbSlot, qb);

        player.openInventory(inv);
    }

    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();
        // check for quick-buy
        if ("Quick Buy".equals(name)) {
            p.sendMessage("&aQuick buy opened (not implemented)");
            return;
        }
        // try to find upgrade by display name
        ShopItem it = shopManager.findByName(name);
        if (it != null) {
            // delegate to ShopService for purchase flow
            if ("upgrade".equalsIgnoreCase(it.getType())) {
                // perform upgrade purchase
                int price = it.getPrice();
                boolean ok = shopManager.consumeCurrency(p, price);
                if (!ok) { p.sendMessage("&cNot enough currency"); return; }
                String upId = it.getUpgradeId();
                Bedwars plugin = Bedwars.getInstance();
                plugin.getPlayerDataManager().getUpgradeLevelAsync(p.getUniqueId(), upId).thenAccept(current -> {
                    int next = current + 1;
                    if (it.getMaxLevel() > 0 && next > it.getMaxLevel()) {
                        Bukkit.getScheduler().runTask(plugin, () -> p.sendMessage("&cUpgrade already at max level"));
                        return;
                    }
                    plugin.getPlayerDataManager().setUpgradeLevelAsync(p.getUniqueId(), upId, next).thenRun(() -> {
                        // run receive actions for that tier if defined in upgrade definitions
                        UpgradeDefinition def = shopManager.getUpgradeDefinitions().get(upId);
                        if (def != null) {
                            // choose tier key like tier-<next>
                            String tierKey = "tier-" + next;
                            UpgradeTier ut = def.getTiers().get(tierKey);
                            if (ut != null) shopService.applyReceiveActions(ut.receive, p, next);
                        }
                        Bukkit.getScheduler().runTask(plugin, () -> p.sendMessage("&aUpgraded " + it.getName() + " to level " + next));
                    });
                });
                return;
            } else {
                // normal item purchase
                int price = it.getPrice();
                boolean ok = shopManager.consumeCurrency(p, price);
                if (!ok) { p.sendMessage("&cNot enough currency"); return; }
                Material mat;
                try { mat = Material.valueOf(it.getMaterial()); } catch (Exception ex) { mat = Material.STONE; }
                ItemStack give = new ItemStack(mat, 1);
                p.getInventory().addItem(give);
                p.sendMessage("&aPurchase successful: " + it.getName());
                new ShopLogger(Bedwars.getInstance()).recordPurchase(p.getUniqueId(), it.getName(), price);
                return;
            }
        }
        // else, click a category or separator
    }
}
