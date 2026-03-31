package org.vaska.bedwars.shop;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.vaska.bedwars.Bedwars;
import java.util.Map;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShopManager {
    private final Bedwars plugin;
    private final List<ShopItem> items = new ArrayList<>();
    private final Map<String, UpgradeDefinition> upgrades = new java.util.HashMap<>();
    private UpgradeConfig upgradeConfig = new UpgradeConfig();

    public ShopManager(Bedwars plugin) { this.plugin = plugin; }

    public void load() {
        try {
            File f = new File(plugin.getDataFolder(), "shop.yml");
            if (!f.exists()) plugin.saveResource("shop.yml", false);
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            items.clear();
            upgrades.clear();
            if (cfg.isConfigurationSection("shop")) {
                for (String cat : cfg.getConfigurationSection("shop").getKeys(false)) {
                    for (Object o : cfg.getList("shop." + cat)) {
                        if (!(o instanceof java.util.Map)) continue;
                        java.util.Map map = (java.util.Map) o;
                        String name = String.valueOf(map.getOrDefault("name","item"));
                        String mat = String.valueOf(map.getOrDefault("material","STONE"));
                        int price = Integer.parseInt(String.valueOf(map.getOrDefault("price", 1)));
                        String type = String.valueOf(map.getOrDefault("type", "item"));
                        String upgradeId = map.containsKey("upgrade_id") ? String.valueOf(map.get("upgrade_id")) : null;
                        int maxLevel = Integer.parseInt(String.valueOf(map.getOrDefault("max_level", 0)));
                        if ("upgrade".equalsIgnoreCase(type) && upgradeId != null) {
                            items.add(new ShopItem(name, mat, price, cat, "upgrade", upgradeId, maxLevel));
                        } else {
                            items.add(new ShopItem(name, mat, price, cat));
                        }
                    }
                }
            }
            // parse upgrade-style sections at root (default-upgrades-settings and upgrade-*)
            if (cfg.isConfigurationSection("default-upgrades-settings")) {
                FileConfiguration uc = cfg.getConfigurationSection("default-upgrades-settings").getValues(true) instanceof java.util.Map ? cfg : cfg;
                // simple reads
                upgradeConfig = new UpgradeConfig();
                upgradeConfig.menuSize = cfg.getInt("default-upgrades-settings.menu-size", 45);
                upgradeConfig.menuContent = cfg.getStringList("default-upgrades-settings.menu-content");
                upgradeConfig.trapStartPrice = cfg.getInt("default-upgrades-settings.trap-start-price", 1);
                upgradeConfig.trapIncrementPrice = cfg.getInt("default-upgrades-settings.trap-increment-price", 1);
                upgradeConfig.trapCurrency = cfg.getString("default-upgrades-settings.trap-currency", "diamond");
                upgradeConfig.trapQueueLimit = cfg.getInt("default-upgrades-settings.trap-queue-limit", 3);
            }
            for (String key : cfg.getKeys(false)) {
                if (key.startsWith("upgrade-")) {
                    // parse upgrade definition
                    if (cfg.isConfigurationSection(key)) {
                        UpgradeDefinition def = new UpgradeDefinition(key);
                        for (String tierKey : cfg.getConfigurationSection(key).getKeys(false)) {
                            if (tierKey.startsWith("tier-") && cfg.isConfigurationSection(key + "." + tierKey)) {
                                int tierNum = 1;
                                try { tierNum = Integer.parseInt(tierKey.replace("tier-", "")); } catch (Exception ignored) {}
                                org.bukkit.configuration.ConfigurationSection sect = cfg.getConfigurationSection(key + "." + tierKey);
                                UpgradeTier tier = new UpgradeTier(tierNum, sect.getInt("cost", 0), sect.getString("currency", "diamond"));
                                if (sect.isConfigurationSection("display-item")) {
                                    org.bukkit.configuration.ConfigurationSection di = sect.getConfigurationSection("display-item");
                                    tier.displayMaterial = di.getString("material", "STONE");
                                    tier.displayAmount = di.getInt("amount", 1);
                                }
                                if (sect.isList("receive")) tier.receive = sect.getStringList("receive");
                                def.getTiers().put(tierKey, tier);
                            }
                        }
                        upgrades.put(key, def);
                    }
                }
            }
            plugin.getLogger().info("Loaded shop items: " + items.size());
        } catch (Exception ex) { plugin.getLogger().severe("Failed to load shop.yml: " + ex.getMessage()); }
    }

    public Map<String, UpgradeDefinition> getUpgradeDefinitions() { return upgrades; }

    public UpgradeConfig getUpgradeConfig() { return upgradeConfig; }

    public boolean consumeCurrency(org.bukkit.entity.Player player, int amount) {
        org.bukkit.Material currency = org.bukkit.Material.IRON_INGOT;
        int remaining = amount;
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            org.bukkit.inventory.ItemStack stack = inv.getItem(i);
            if (stack == null) continue;
            if (stack.getType() == currency) {
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
                if (stack.getAmount() <= 0) inv.setItem(i, null);
                if (remaining <= 0) break;
            }
        }
        player.updateInventory();
        return remaining <= 0;
    }

    public ShopItem findByName(String name) {
        for (ShopItem it : items) if (it.getName().equalsIgnoreCase(name)) return it;
        return null;
    }

    public List<ShopItem> getItems() { return items; }
}
