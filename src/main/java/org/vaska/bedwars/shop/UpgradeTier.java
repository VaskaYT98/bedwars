package org.vaska.bedwars.shop;

import java.util.List;

public class UpgradeTier {
    public final int tier;
    public final int cost;
    public final String currency;
    public String displayMaterial = "STONE";
    public int displayAmount = 1;
    public List<String> receive = java.util.Collections.emptyList();

    public UpgradeTier(int tier, int cost, String currency) {
        this.tier = tier; this.cost = cost; this.currency = currency;
    }
}
