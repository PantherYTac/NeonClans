package com.neonclans.manager;

import com.neonclans.NeonClans;
import com.neonclans.model.ClanPerk;

import java.util.*;

public class PerkManager {
    private final NeonClans plugin;
    private final Map<String, Map<String, ClanPerk>> clanPerksMap = new HashMap<>(); // clanId -> (perkId -> ClanPerk)

    public PerkManager(NeonClans plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("features.perks.enabled", true);
    }

    public void load() {
        if (!isEnabled()) return;
        clanPerksMap.clear();
        clanPerksMap.putAll(plugin.getDatabaseManager().loadPerks());
    }

    public int getPerkLevel(String clanId, String perkId) {
        if (!isEnabled()) return 0;
        Map<String, ClanPerk> map = clanPerksMap.get(clanId);
        if (map != null) {
            ClanPerk perk = map.get(perkId);
            return perk != null ? perk.getLevel() : 0;
        }
        return 0;
    }

    public boolean hasPerk(String clanId, String perkId) {
        return getPerkLevel(clanId, perkId) > 0;
    }

    public void upgradePerk(String clanId, String perkId) {
        if (!isEnabled()) return;
        Map<String, ClanPerk> map = clanPerksMap.computeIfAbsent(clanId, k -> new HashMap<>());
        ClanPerk perk = map.get(perkId);
        if (perk == null) {
            perk = new ClanPerk(clanId, perkId, 1);
            map.put(perkId, perk);
        } else {
            perk.setLevel(perk.getLevel() + 1);
        }

        ClanPerk finalPerk = perk;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().savePerk(finalPerk)
        );
    }
}
