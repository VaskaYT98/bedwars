package org.vaska.bedwars.shop;

import org.vaska.bedwars.Bedwars;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps upgrade definitions and helpers.
 */
public class UpgradeManager {
    private final Bedwars plugin;
    private final Map<String, UpgradeDefinition> upgrades = new HashMap<>();

    public UpgradeManager(Bedwars plugin) {
        this.plugin = plugin;
    }

    public void reloadFromShop(ShopManager shopManager) {
        upgrades.clear();
        // load modern shop.yml upgrade-* definitions
        for (java.util.Map.Entry<String, UpgradeDefinition> e : shopManager.getUpgradeDefinitions().entrySet()) {
            upgrades.put(e.getKey(), e.getValue());
        }
        // also fallback to old shop items with type=upgrade
        for (ShopItem it : shopManager.getItems()) {
            if ("upgrade".equalsIgnoreCase(it.getType()) && it.getUpgradeId() != null) {
                // wrap simple ShopItem into UpgradeDefinition
                UpgradeDefinition def = new UpgradeDefinition(it.getUpgradeId());
                UpgradeTier t = new UpgradeTier(1, it.getPrice(), "diamond");
                t.displayMaterial = it.getMaterial();
                def.getTiers().put("tier-1", t);
                upgrades.put(it.getUpgradeId(), def);
            }
        }
        plugin.getLogger().info("Loaded upgrades: " + upgrades.size());
    }

    public UpgradeDefinition getUpgrade(String id) { return upgrades.get(id); }
}
