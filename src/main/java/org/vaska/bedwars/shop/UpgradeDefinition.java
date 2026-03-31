package org.vaska.bedwars.shop;

import java.util.LinkedHashMap;
import java.util.Map;

public class UpgradeDefinition {
    private final String id;
    private final Map<String, UpgradeTier> tiers = new LinkedHashMap<>();

    public UpgradeDefinition(String id) { this.id = id; }

    public String getId() { return id; }

    public Map<String, UpgradeTier> getTiers() { return tiers; }
}
