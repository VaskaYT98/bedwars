package org.vaska.bedwars.shop;

public class ShopItem {
    private final String name;
    private final String material;
    private final int price;
    private final String category;
    private final String type; // "item" or "upgrade"
    private final String upgradeId; // optional id for upgrades
    private final int maxLevel;

    public ShopItem(String name, String material, int price, String category) {
        this(name, material, price, category, "item", null, 0);
    }

    public ShopItem(String name, String material, int price, String category, String type, String upgradeId, int maxLevel) {
        this.name = name; this.material = material; this.price = price; this.category = category;
        this.type = type == null ? "item" : type;
        this.upgradeId = upgradeId;
        this.maxLevel = maxLevel;
    }

    public String getName() { return name; }
    public String getMaterial() { return material; }
    public int getPrice() { return price; }
    public String getCategory() { return category; }
    public String getType() { return type; }
    public String getUpgradeId() { return upgradeId; }
    public int getMaxLevel() { return maxLevel; }
}
