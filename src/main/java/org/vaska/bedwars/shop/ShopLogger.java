package org.vaska.bedwars.shop;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.vaska.bedwars.Bedwars;

import java.io.File;
import java.util.UUID;

public class ShopLogger {
    private final Bedwars plugin;
    private final File file;
    private final FileConfiguration cfg;

    public ShopLogger(Bedwars plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "purchases.yml");
        if (!file.exists()) { try { plugin.saveResource("purchases.yml", false); } catch (Exception ignored) {} }
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void recordPurchase(UUID player, String itemName, int price) {
        String path = player.toString() + ".count";
        int cur = cfg.getInt(path, 0);
        cfg.set(path, cur + 1);
        cfg.set(player.toString() + ".lastItem", itemName);
        cfg.set(player.toString() + ".lastPrice", price);
        try { cfg.save(file); } catch (Exception ex) { plugin.getLogger().severe("Failed to save purchases.yml: " + ex.getMessage()); }
        plugin.getLogger().info("[Shop] " + player + " purchased " + itemName + " for " + price);
    }
}
