package com.neonclans.gui;

import com.neonclans.NeonClans;
import com.neonclans.model.Clan;
import com.neonclans.model.ClanMember;
import com.neonclans.model.ClanRole;
import com.neonclans.util.InventorySerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class ClanMenu {
    private final NeonClans plugin;

    public ClanMenu(NeonClans plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan! Use /clan create <name> to start one.");
            return;
        }

        ClanMenuHolder holder = new ClanMenuHolder(event -> {
            int slot = event.getRawSlot();
            if (slot == 10) {
                player.closeInventory();
                player.performCommand("clan info");
            } else if (slot == 12) {
                openMemberListMenu(player, clan);
            } else if (slot == 14) {
                openUpgradeMenu(player, clan);
            } else if (slot == 16) {
                ClanMember member = clan.getMember(player.getUniqueId());
                if (member != null && (member.getRole() == ClanRole.OWNER || member.getRole() == ClanRole.MODERATOR)) {
                    boolean current = clan.isFriendlyFireEnabled();
                    plugin.getClanManager().setFriendlyFire(clan, !current);
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Friendly Fire toggled to: " + (!current ? "ENABLED" : "DISABLED"));
                    openMainMenu(player);
                } else {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only the Clan Owner or Moderators can toggle friendly fire.");
                }
            } else if (slot == 19) {
                openTopClansMenu(player);
            } else if (slot == 21) {
                openQuestsMenu(player, clan);
            } else if (slot == 23) {
                openPerksMenu(player, clan);
            } else if (slot == 25) {
                openAuditLogsMenu(player, clan);
            } else if (slot == 31) {
                openClanVault(player, clan);
            }
        });

        Inventory inv = Bukkit.createInventory(holder, 36, ChatColor.DARK_GRAY + "Clan: " + ChatColor.DARK_PURPLE + clan.getName());
        holder.setInventory(inv);

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, border);
        }

        // Slot 10: Info
        ItemStack infoItem = createItem(Material.BOOK, "&b&lClan Info",
                "&7Name: &d" + clan.getName(),
                "&7Tag: &e" + clan.getTag(),
                "&7Level: &a" + clan.getLevel() + " &8/ &a" + plugin.getClanManager().getMaxLevel(),
                "&7Bank Balance: &a$" + String.format("%.2f", clan.getBalance()),
                "&7Members: &f" + clan.getMembers().size() + " &8/ &f" + plugin.getClanManager().getMaxMembers(clan.getLevel()),
                "&7Allies: &f" + clan.getAllies().size() + " &8/ &f" + plugin.getClanManager().getMaxAllies(clan.getLevel()),
                "&7Friendly Fire: " + (clan.isFriendlyFireEnabled() ? "&aEnabled" : "&cDisabled"),
                "",
                "&eClick to view details in chat!"
        );
        inv.setItem(10, infoItem);

        // Slot 12: Members list
        ItemStack membersItem = createItem(Material.PLAYER_HEAD, "&a&lClan Members",
                "&7Click to manage members,",
                "&7promote/demote, or invite players."
        );
        SkullMeta skullMeta = (SkullMeta) membersItem.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(clan.getOwnerUuid()));
            membersItem.setItemMeta(skullMeta);
        }
        inv.setItem(12, membersItem);

        // Slot 14: Upgrades
        ItemStack upgradesItem = createItem(Material.EMERALD, "&e&lClan Upgrades",
                "&7Click to view clan level details,",
                "&7costs, and purchase upgrades."
        );
        inv.setItem(14, upgradesItem);

        // Slot 16: Friendly Fire Toggle
        ItemStack ffItem = createItem(
                clan.isFriendlyFireEnabled() ? Material.REDSTONE : Material.GLOWSTONE_DUST,
                "&c&lFriendly Fire",
                "&7Status: " + (clan.isFriendlyFireEnabled() ? "&aENABLED" : "&cDISABLED"),
                "",
                "&eClick to toggle Friendly Fire!"
        );
        inv.setItem(16, ffItem);

        // Row 3: Features
        inv.setItem(19, createItem(Material.GOLD_BLOCK, "&6&lTop Clans Leaderboard", "&7View ranking of top clans."));
        inv.setItem(21, createItem(Material.WRITABLE_BOOK, "&e&lDaily Quests", "&7View daily objectives and rewards."));
        inv.setItem(23, createItem(Material.NETHER_STAR, "&b&lClan Skill Perks", "&7Unlock passive clan skills."));
        inv.setItem(25, createItem(Material.PAPER, "&f&lAudit Logs", "&7View recent clan activity logs."));

        // Slot 31: Vault
        ItemStack vaultItem = createItem(Material.CHEST, "&d&lClan Vault",
                "&7Storage Size: &e" + (plugin.getClanManager().getVaultRows(clan.getLevel()) * 9) + " slots",
                "",
                "&eClick to open clan storage vault!"
        );
        inv.setItem(31, vaultItem);

        player.openInventory(inv);
    }

    public void openMemberListMenu(Player player, Clan clan) {
        ClanMenuHolder holder = new ClanMenuHolder(event -> {
            int slot = event.getRawSlot();
            if (slot == 45) {
                // Back to main menu
                openMainMenu(player);
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.PLAYER_HEAD) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.getDisplayName() != null) {
                    String targetName = ChatColor.stripColor(meta.getDisplayName());
                    OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                    
                    if (target.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You cannot manage yourself!");
                        return;
                    }
                    
                    ClanMember selfMember = clan.getMember(player.getUniqueId());
                    ClanMember targetMember = clan.getMember(target.getUniqueId());
                    
                    if (selfMember == null || targetMember == null) return;

                    // Check permissions
                    if (selfMember.getRole() == ClanRole.MEMBER) {
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to manage members.");
                        return;
                    }
                    if (selfMember.getRole() == ClanRole.MODERATOR && targetMember.getRole() != ClanRole.MEMBER) {
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Moderators can only manage normal members.");
                        return;
                    }

                    // Open action menu
                    openMemberActionMenu(player, clan, target);
                }
            }
        });

        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_GRAY + "Members: " + clan.getName());
        holder.setInventory(inv);

        // Border
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, border);
        }
        inv.setItem(45, createItem(Material.BARRIER, "&cBack to Menu"));

        // Add member skull items
        int index = 0;
        for (ClanMember member : clan.getMembers().values()) {
            if (index >= 45) break;

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member.getUuid());
            String roleName = "";
            String roleColor = "";
            
            switch (member.getRole()) {
                case OWNER:
                    roleName = "Owner";
                    roleColor = "&c&l";
                    break;
                case MODERATOR:
                    roleName = "Moderator";
                    roleColor = "&b&l";
                    break;
                case MEMBER:
                    roleName = "Member";
                    roleColor = "&7";
                    break;
            }

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(offlinePlayer);
                meta.setDisplayName(roleColor + offlinePlayer.getName());
                meta.setLore(Arrays.asList(
                        ChatColor.translateAlternateColorCodes('&', "&7Role: " + roleColor + roleName),
                        ChatColor.translateAlternateColorCodes('&', "&7Status: " + (offlinePlayer.isOnline() ? "&aOnline" : "&7Offline")),
                        ChatColor.translateAlternateColorCodes('&', "&7Joined at: &f" + new Date(member.getJoinedAt())),
                        "",
                        ChatColor.translateAlternateColorCodes('&', "&eClick to manage this member!")
                ));
                skull.setItemMeta(meta);
            }
            inv.setItem(index++, skull);
        }

        player.openInventory(inv);
    }

    private void openMemberActionMenu(Player player, Clan clan, OfflinePlayer target) {
        ClanMember targetMember = clan.getMember(target.getUniqueId());
        if (targetMember == null) return;

        ClanMenuHolder holder = new ClanMenuHolder(event -> {
            int slot = event.getRawSlot();
            if (slot == 4) {
                openMemberListMenu(player, clan);
            } else if (slot == 11) {
                // Promote / Demote
                if (targetMember.getRole() == ClanRole.MEMBER) {
                    // Check mod count limit
                    int mods = clan.getModeratorCount();
                    int maxMods = plugin.getConfig().getInt("roles.moderator.max-promotions", 3);
                    if (mods >= maxMods) {
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Maximum moderators limit reached (" + maxMods + "). Demote someone first.");
                        return;
                    }
                    plugin.getClanManager().promoteMember(clan, target.getUniqueId());
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + target.getName() + " has been promoted to Moderator.");
                } else if (targetMember.getRole() == ClanRole.MODERATOR) {
                    plugin.getClanManager().demoteMember(clan, target.getUniqueId());
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + target.getName() + " has been demoted to Member.");
                }
                player.closeInventory();
            } else if (slot == 15) {
                // Kick
                plugin.getClanManager().removeMemberFromClan(clan, target.getUniqueId());
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + target.getName() + " has been kicked from the clan.");
                
                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null && onlineTarget.isOnline()) {
                    onlineTarget.sendMessage(plugin.getPrefix() + ChatColor.RED + "You have been kicked from " + clan.getName());
                }
                player.closeInventory();
            }
        });

        Inventory inv = Bukkit.createInventory(holder, 18, ChatColor.DARK_GRAY + "Manage: " + target.getName());
        holder.setInventory(inv);

        // Fill background
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 18; i++) {
            inv.setItem(i, border);
        }

        inv.setItem(4, createItem(Material.BARRIER, "&cBack to Member List"));

        String promoteDemoteName = targetMember.getRole() == ClanRole.MEMBER ? "&a&lPromote" : "&e&lDemote";
        String promoteDemoteLore = targetMember.getRole() == ClanRole.MEMBER ? "&7Promote this member to Moderator" : "&7Demote this moderator to Member";
        inv.setItem(11, createItem(Material.SUNFLOWER, promoteDemoteName, promoteDemoteLore));

        inv.setItem(15, createItem(Material.TNT, "&c&lKick Member", "&7Kick this member from the clan"));

        player.openInventory(inv);
    }

    public void openUpgradeMenu(Player player, Clan clan) {
        ClanMenuHolder holder = new ClanMenuHolder(event -> {
            int slot = event.getRawSlot();
            if (slot == 8) {
                openMainMenu(player);
                return;
            }

            int currentLevel = clan.getLevel();
            int maxLevel = plugin.getClanManager().getMaxLevel();

            if (slot == 13) {
                if (currentLevel >= maxLevel) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Your clan is already at the maximum level!");
                    return;
                }

                int nextLevel = currentLevel + 1;
                double cost = plugin.getClanManager().getUpgradeCost(nextLevel);

                if (!plugin.getVaultHook().has(player, cost)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have enough money to buy this upgrade! Cost: $" + cost);
                    return;
                }

                // Complete upgrade
                plugin.getVaultHook().withdraw(player, cost);
                clan.setLevel(nextLevel);
                
                // Save database
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
                    plugin.getDatabaseManager().saveClan(clan)
                );

                // Broadcast
                String format = plugin.getPrefix() + ChatColor.translateAlternateColorCodes('&', 
                        "&eClan &d" + clan.getName() + " &eupgraded to Level &d" + nextLevel + "&e! Color format changed.");
                for (ClanMember member : clan.getMembers().values()) {
                    Player online = Bukkit.getPlayer(member.getUuid());
                    if (online != null && online.isOnline()) {
                        online.sendMessage(format);
                    }
                }

                openUpgradeMenu(player, clan); // refresh
            }
        });

        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_GRAY + "Clan Upgrades");
        holder.setInventory(inv);

        // Border
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }
        inv.setItem(8, createItem(Material.BARRIER, "&cBack to Menu"));

        int level = clan.getLevel();
        int maxLvl = plugin.getClanManager().getMaxLevel();

        // Level Status Info
        ItemStack statusItem = createItem(Material.BOOK, "&a&lUpgrade Summary",
                "&7Current Level: &e" + level + " &8/ &e" + maxLvl,
                "&7Current Max Members: &e" + plugin.getClanManager().getMaxMembers(level),
                "&7Current Max Allies: &e" + plugin.getClanManager().getMaxAllies(level),
                "&7Current Vault Size: &e" + (plugin.getClanManager().getVaultRows(level) * 9) + " slots"
        );
        inv.setItem(11, statusItem);

        // Upgrade Action Button
        if (level < maxLvl) {
            int nextLvl = level + 1;
            double cost = plugin.getClanManager().getUpgradeCost(nextLvl);
            int nextMembers = plugin.getClanManager().getMaxMembers(nextLvl);
            int nextAllies = plugin.getClanManager().getMaxAllies(nextLvl);
            int nextVault = plugin.getClanManager().getVaultRows(nextLvl) * 9;
            String nextColor = plugin.getConfig().getString("upgrades." + nextLvl + ".tag-color", "&7");

            ItemStack actionItem = createItem(Material.EMERALD_BLOCK, "&e&lUpgrade to Level " + nextLvl,
                    "&7Requirements:",
                    " &8- &7Cost: &a$" + String.format("%.2f", cost),
                    "",
                    "&7Rewards unlocked:",
                    " &8- &7Max Members: &e" + nextMembers + " &8(+" + (nextMembers - plugin.getClanManager().getMaxMembers(level)) + ")",
                    " &8- &7Max Allies: &e" + nextAllies,
                    " &8- &7Vault Size: &e" + nextVault + " slots",
                    " &8- &7Tag Color: " + nextColor + "This Color",
                    "",
                    "&eClick to purchase this upgrade!"
            );
            inv.setItem(13, actionItem);
        } else {
            ItemStack maxItem = createItem(Material.NETHERITE_BLOCK, "&d&lClan Maxed Out!",
                    "&7Your clan is at the maximum level!",
                    "&7Unlocked privileges:",
                    " &8- &dCustomizable Tags (/clan tag)",
                    " &8- &7Max Members: &e" + plugin.getClanManager().getMaxMembers(level),
                    " &8- &7Double Chest Clan Vault (54 slots)"
            );
            inv.setItem(13, maxItem);
        }

        // Quick Stats / Info for next levels
        ItemStack statsItem = createItem(Material.COMPASS, "&b&lAll Upgrade Levels",
                "&7Lvl 1: &fFree &8| &75 Members &8| &79 slot Vault",
                "&7Lvl 2: &f$500 &8| &710 Members &8| &718 slot Vault",
                "&7Lvl 3: &f$2500 &8| &715 Members &8| &727 slot Vault",
                "&7Lvl 4: &f$10000 &8| &720 Members &8| &736 slot Vault",
                "&7Lvl 5: &f$50000 &8| &730 Members &8| &754 slot Vault"
        );
        inv.setItem(15, statsItem);

        player.openInventory(inv);
    }

    public void openClanVault(Player player, Clan clan) {
        int rows = plugin.getClanManager().getVaultRows(clan.getLevel());
        
        ClanVaultHolder holder = new ClanVaultHolder(clan);
        Inventory vaultInv = Bukkit.createInventory(holder, rows * 9, ChatColor.DARK_PURPLE + clan.getName() + " Vault");
        holder.setInventory(vaultInv);

        // Deserialise data
        ItemStack[] items = InventorySerializer.itemStackArrayFromBase64(clan.getVaultData());
        if (items.length > 0) {
            // Trim or expand array to match current row capacity
            ItemStack[] adjustedItems = new ItemStack[rows * 9];
            System.arraycopy(items, 0, adjustedItems, 0, Math.min(items.length, adjustedItems.length));
            vaultInv.setContents(adjustedItems);
        }

        player.openInventory(vaultInv);
    }

    public void openTopClansMenu(Player player) {
        ClanMenuHolder holder = new ClanMenuHolder(event -> {
            if (event.getRawSlot() == 45) {
                openMainMenu(player);
            }
        });

        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.GOLD + "Top Clans Leaderboard");
        holder.setInventory(inv);

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, border);
        }
        inv.setItem(45, createItem(Material.BARRIER, "&cBack to Menu"));

        List<Clan> sorted = new ArrayList<>(plugin.getClanManager().getClans().values());
        sorted.sort((c1, c2) -> {
            if (c2.getLevel() != c1.getLevel()) {
                return Integer.compare(c2.getLevel(), c1.getLevel());
            }
            return Double.compare(c2.getBalance(), c1.getBalance());
        });

        int index = 0;
        for (Clan c : sorted) {
            if (index >= 45) break;

            OfflinePlayer owner = Bukkit.getOfflinePlayer(c.getOwnerUuid());
            String ownerName = owner.getName() != null ? owner.getName() : "Unknown";

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(owner);
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6#" + (index + 1) + " &e" + c.getName()));
                meta.setLore(Arrays.asList(
                        ChatColor.translateAlternateColorCodes('&', "&7Tag: &d" + c.getTag()),
                        ChatColor.translateAlternateColorCodes('&', "&7Owner: &f" + ownerName),
                        ChatColor.translateAlternateColorCodes('&', "&7Level: &a" + c.getLevel()),
                        ChatColor.translateAlternateColorCodes('&', "&7Bank: &a$" + String.format("%.2f", c.getBalance())),
                        ChatColor.translateAlternateColorCodes('&', "&7Members: &f" + c.getMembers().size())
                ));
                skull.setItemMeta(meta);
            }
            inv.setItem(index++, skull);
        }

        player.openInventory(inv);
    }

    public void openQuestsMenu(Player player, Clan clan) {
        ClanMenuHolder holder = new ClanMenuHolder(event -> {
            if (event.getRawSlot() == 18) {
                openMainMenu(player);
            }
        });

        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_GRAY + "Clan Daily Quests");
        holder.setInventory(inv);

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }
        inv.setItem(18, createItem(Material.BARRIER, "&cBack to Menu"));

        com.neonclans.model.ClanQuest quest = plugin.getQuestManager().getQuest(clan.getId());
        if (quest != null) {
            String typeName = quest.getQuestType().replace("_", " ");
            double percent = Math.min(100.0, ((double) quest.getCurrentProgress() / quest.getTargetAmount()) * 100.0);

            ItemStack questItem = createItem(
                    quest.isCompleted() ? Material.EMERALD_BLOCK : Material.BOOK,
                    "&e&lDaily Objective: " + typeName,
                    "&7Target Amount: &f" + quest.getTargetAmount(),
                    "&7Current Progress: &a" + quest.getCurrentProgress() + " &8(" + String.format("%.1f", percent) + "%)",
                    "&7Status: " + (quest.isCompleted() ? "&aCOMPLETED" : "&eIN PROGRESS"),
                    "",
                    "&7Reward: &a$" + plugin.getConfig().getDouble("features.quests.reward-money", 1000.0) + " Clan Bank"
            );
            inv.setItem(13, questItem);
        }

        player.openInventory(inv);
    }

    public void openPerksMenu(Player player, Clan clan) {
        ClanMenuHolder holder = new ClanMenuHolder(event -> {
            int slot = event.getRawSlot();
            if (slot == 18) {
                openMainMenu(player);
                return;
            }

            if (slot == 10 || slot == 12 || slot == 14 || slot == 16) {
                String perkId = slot == 10 ? "loot_harvester" : slot == 12 ? "teleport_haste" : slot == 14 ? "exp_share" : "vault_protection";
                ClanMember member = clan.getMember(player.getUniqueId());
                if (member != null && (member.getRole() == ClanRole.OWNER || member.getRole() == ClanRole.MODERATOR)) {
                    plugin.getPerkManager().upgradePerk(clan.getId(), perkId);
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Perk upgraded successfully!");
                    openPerksMenu(player, clan);
                } else {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only Owners/Moderators can upgrade perks.");
                }
            }
        });

        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_GRAY + "Clan Skill Tree");
        holder.setInventory(inv);

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }
        inv.setItem(18, createItem(Material.BARRIER, "&cBack to Menu"));

        boolean loot = plugin.getPerkManager().hasPerk(clan.getId(), "loot_harvester");
        boolean tele = plugin.getPerkManager().hasPerk(clan.getId(), "teleport_haste");
        boolean exp = plugin.getPerkManager().hasPerk(clan.getId(), "exp_share");
        boolean vprot = plugin.getPerkManager().hasPerk(clan.getId(), "vault_protection");

        inv.setItem(10, createItem(Material.GOLDEN_HOE, "&e&lLoot Harvester", "&7+15% Mob EXP & Loot Drops", "", "Status: " + (loot ? "&aUnlocked" : "&cLocked (Click to Unlock)")));
        inv.setItem(12, createItem(Material.FEATHER, "&b&lTeleport Haste", "&750% faster clan home teleport", "", "Status: " + (tele ? "&aUnlocked" : "&cLocked (Click to Unlock)")));
        inv.setItem(14, createItem(Material.EXPERIENCE_BOTTLE, "&a&lXP Sharing", "&7Shares 10% EXP with nearby members", "", "Status: " + (exp ? "&aUnlocked" : "&cLocked (Click to Unlock)")));
        inv.setItem(16, createItem(Material.IRON_DOOR, "&c&lVault Protection", "&7Restricts vault access to staff", "", "Status: " + (vprot ? "&aUnlocked" : "&cLocked (Click to Unlock)")));

        player.openInventory(inv);
    }

    public void openAuditLogsMenu(Player player, Clan clan) {
        ClanMenuHolder holder = new ClanMenuHolder(event -> {
            if (event.getRawSlot() == 45) {
                openMainMenu(player);
            }
        });

        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_GRAY + "Clan Audit Logs");
        holder.setInventory(inv);

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, border);
        }
        inv.setItem(45, createItem(Material.BARRIER, "&cBack to Menu"));

        List<com.neonclans.model.ClanLog> logs = plugin.getLogManager().getLogs(clan.getId());
        int index = 0;
        for (com.neonclans.model.ClanLog log : logs) {
            if (index >= 45) break;

            ItemStack item = createItem(Material.PAPER, "&d" + log.getActionType(),
                    "&7Player: &f" + log.getPlayerName(),
                    "&7Details: &e" + log.getDetails(),
                    "&7Date: &8" + new Date(log.getTimestamp())
            );
            inv.setItem(index++, item);
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                List<String> list = new ArrayList<>();
                for (String l : lore) {
                    list.add(ChatColor.translateAlternateColorCodes('&', l));
                }
                meta.setLore(list);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
