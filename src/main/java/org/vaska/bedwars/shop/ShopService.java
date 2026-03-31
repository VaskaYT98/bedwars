package org.vaska.bedwars.shop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.vaska.bedwars.Bedwars;

import java.util.*;

/**
 * Central purchase and receive-action executor. Handles upgrade levels, trap queues and applying effects.
 */
public class ShopService {
    private final Bedwars plugin;
    private final ShopManager shopManager;
    // simple trap queue per team or player
    private final Map<UUID, Deque<String>> trapQueue = new HashMap<>();

    public ShopService(Bedwars plugin, ShopManager shopManager) {
        this.plugin = plugin; this.shopManager = shopManager;
    }

    public boolean consumeCurrencySync(Player player, int amount, String currencyName) {
        // reuse ShopManager currency consumer for now (assumes iron ingot); currencies in config not fully supported yet
        return shopManager.consumeCurrency(player, amount);
    }

    public void applyReceiveActions(List<String> actions, Player actor, int appliedTier) {
        if (actions == null) return;
        for (String a : actions) {
            parseAndExecute(a, actor, appliedTier);
        }
    }

    private void parseAndExecute(String raw, Player actor, int tier) {
        try {
            String s = raw.trim();
            if (s.startsWith("enchant-item:")) {
                String args = s.substring("enchant-item:".length()).trim();
                // format: ENCHANT_NAME,level,target
                String[] parts = args.split(",");
                String ench = parts[0].trim();
                int lvl = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
                String target = parts.length > 2 ? parts[2].trim() : "sword";
                Enchantment e = Enchantment.getByName(ench);
                if (e != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (target.equalsIgnoreCase("armor")) {
                            actor.getInventory().getArmorContents();
                            // apply to chestplate if present
                            if (actor.getInventory().getChestplate() != null) actor.getInventory().getChestplate().addEnchantment(e, lvl);
                        } else {
                            if (actor.getInventory().getItemInMainHand() != null) actor.getInventory().getItemInMainHand().addEnchantment(e, lvl);
                        }
                    });
                }
            } else if (s.startsWith("player-effect:")) {
                String args = s.substring("player-effect:".length()).trim();
                // format: EFFECT,amplifier,duration,target
                String[] parts = args.split(",");
                String eff = parts[0].trim();
                int amp = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                int dur = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 30;
                String target = parts.length > 3 ? parts[3].trim() : "player";
                PotionEffectType pet = PotionEffectType.getByName(eff);
                if (pet != null) {
                    if (target.equalsIgnoreCase("player") || target.equalsIgnoreCase("base") ) {
                        Bukkit.getScheduler().runTask(plugin, () -> actor.addPotionEffect(new PotionEffect(pet, dur * 20, amp)));
                    } else if (target.equalsIgnoreCase("team")) {
                        plugin.getArenaManager().getArenaForPlayer(actor.getUniqueId()).ifPresent(a -> {
                            int team = a.getTeam(actor.getUniqueId());
                            a.getPlayers().forEach(uuid -> {
                                if (a.getTeam(uuid) == team) {
                                    Player p = Bukkit.getPlayer(uuid);
                                    if (p != null) Bukkit.getScheduler().runTask(plugin, () -> p.addPotionEffect(new PotionEffect(pet, dur * 20, amp)));
                                }
                            });
                        });
                    } else if (target.equalsIgnoreCase("enemy")) {
                        plugin.getArenaManager().getArenaForPlayer(actor.getUniqueId()).ifPresent(a -> {
                            int teamA = a.getTeam(actor.getUniqueId());
                            a.getPlayers().forEach(uuid -> {
                                if (a.getTeam(uuid) != teamA) {
                                    Player p = Bukkit.getPlayer(uuid);
                                    if (p != null) Bukkit.getScheduler().runTask(plugin, () -> p.addPotionEffect(new PotionEffect(pet, dur * 20, amp)));
                                }
                            });
                        });
                    }
                }
            } else if (s.startsWith("remove-effect:")) {
                String args = s.substring("remove-effect:".length()).trim();
                String[] parts = args.split(",");
                String eff = parts[0].trim();
                PotionEffectType pet = PotionEffectType.getByName(eff);
                if (pet != null) {
                    if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("enemy")) {
                        plugin.getArenaManager().getArenaForPlayer(actor.getUniqueId()).ifPresent(a -> {
                            int teamA = a.getTeam(actor.getUniqueId());
                            a.getPlayers().forEach(uuid -> {
                                if (a.getTeam(uuid) != teamA) {
                                    Player p = Bukkit.getPlayer(uuid);
                                    if (p != null) Bukkit.getScheduler().runTask(plugin, () -> p.removePotionEffect(pet));
                                }
                            });
                        });
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> actor.removePotionEffect(pet));
                    }
                }
            } else if (s.startsWith("generator-edit:")) {
                // generator-edit: id,rate,slots,x
                // delegate to generator manager if present
                if (plugin.getGeneratorManager() != null) {
                    // for now, just log and let generator manager implement api
                    plugin.getLogger().info("Generator edit requested: " + s);
                }
            } else if (s.startsWith("dragon:")) {
                // spawn or affect dragon count
                plugin.getLogger().info("Dragon action: " + s);
            } else {
                // unknown action: allow console commands or player commands in future
                plugin.getLogger().info("Unknown shop receive action: " + s);
            }
        } catch (Throwable t) { plugin.getLogger().warning("Failed to execute shop action '" + raw + "': " + t.getMessage()); }
    }

    public boolean addTrapToQueue(Player p, String trapId) {
        UUID teamKey = getQueueKeyForPlayer(p);
        Deque<String> q = trapQueue.computeIfAbsent(teamKey, k -> new ArrayDeque<>());
        int limit = shopManager.getUpgradeConfig().trapQueueLimit;
        if (q.size() >= limit) return false;
        q.addLast(trapId);
        return true;
    }

    private UUID getQueueKeyForPlayer(Player p) {
        // group by arena+team if possible, otherwise per-player
        return plugin.getArenaManager().getArenaForPlayer(p.getUniqueId()).map(a -> UUID.nameUUIDFromBytes((a.getId()+"-team"+a.getTeam(p.getUniqueId())).getBytes())).orElse(p.getUniqueId());
    }
}
