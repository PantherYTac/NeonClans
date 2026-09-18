package com.neonclans.manager;

import com.neonclans.NeonClans;
import com.neonclans.model.Clan;
import com.neonclans.model.ClanMember;
import com.neonclans.model.ClanQuest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class QuestManager {
    private final NeonClans plugin;
    private final Map<String, ClanQuest> questMap = new HashMap<>(); // clanId -> ClanQuest
    private final String[] questTypes = {"MINE_BLOCKS", "KILL_MOBS", "PVP_KILLS"};

    public QuestManager(NeonClans plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("features.quests.enabled", true);
    }

    public void load() {
        if (!isEnabled()) return;
        questMap.clear();
        questMap.putAll(plugin.getDatabaseManager().loadQuests());
    }

    public ClanQuest getQuest(String clanId) {
        if (!isEnabled()) return null;
        ClanQuest quest = questMap.get(clanId);
        if (quest == null) {
            quest = generateNewQuest(clanId);
        }
        return quest;
    }

    public ClanQuest generateNewQuest(String clanId) {
        Random rand = new Random();
        String type = questTypes[rand.nextInt(questTypes.length)];
        int target = 100 + rand.nextInt(400); // 100 - 500
        
        ClanQuest quest = new ClanQuest(clanId, type, target, 0, false);
        questMap.put(clanId, quest);
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveQuest(quest)
        );
        return quest;
    }

    public void incrementQuestProgress(Clan clan, String questType, int amount) {
        if (!isEnabled() || clan == null) return;

        ClanQuest quest = getQuest(clan.getId());
        if (quest == null || quest.isCompleted()) return;

        if (quest.getQuestType().equalsIgnoreCase(questType)) {
            boolean wasCompleted = quest.isCompleted();
            quest.incrementProgress(amount);

            if (!wasCompleted && quest.isCompleted()) {
                // Reward clan!
                double moneyReward = plugin.getConfig().getDouble("features.quests.reward-money", 1000.0);
                clan.deposit(moneyReward);
                
                // Save database
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getDatabaseManager().saveQuest(quest);
                    plugin.getDatabaseManager().saveClan(clan);
                });

                String msg = plugin.getPrefix() + ChatColor.GREEN + "DAILY QUEST COMPLETED! &eReward: $" + moneyReward + " deposited into Clan Bank!";
                for (ClanMember m : clan.getMembers().values()) {
                    Player online = Bukkit.getPlayer(m.getUuid());
                    if (online != null && online.isOnline()) {
                        online.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    }
                }
            } else {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
                    plugin.getDatabaseManager().saveQuest(quest)
                );
            }
        }
    }
}
