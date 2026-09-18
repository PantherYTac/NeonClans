package com.neonclans.command;

import com.neonclans.NeonClans;
import com.neonclans.gui.ClanMenu;
import com.neonclans.listener.ChatListener;
import com.neonclans.model.Clan;
import com.neonclans.model.ClanMember;
import com.neonclans.model.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ClanCommand implements CommandExecutor, TabCompleter {
    private final NeonClans plugin;
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private final Map<UUID, BukkitTask> activeTeleports = new HashMap<>();

    public ClanCommand(NeonClans plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                plugin.reloadPlugin();
                sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Configuration and database reloaded!");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Open GUI
            new ClanMenu(plugin).openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "menu":
            case "gui":
                new ClanMenu(plugin).openMainMenu(player);
                break;
            case "create":
                handleCreate(player, args);
                break;
            case "disband":
                handleDisband(player);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "join":
                handleJoin(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "promote":
                handlePromote(player, args);
                break;
            case "demote":
                handleDemote(player, args);
                break;
            case "transfer":
                handleTransfer(player, args);
                break;
            case "sethome":
                handleSetHome(player);
                break;
            case "home":
                handleHome(player);
                break;
            case "friendlyfire":
            case "ff":
                handleFriendlyFire(player);
                break;
            case "bank":
                handleBank(player, args);
                break;
            case "ally":
                handleAlly(player, args);
                break;
            case "tag":
                handleTag(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "chat":
            case "c":
                handleChat(player, args);
                break;
            case "top":
            case "leaderboard":
                new ClanMenu(plugin).openTopClansMenu(player);
                break;
            case "nexus":
                handleNexus(player, args);
                break;
            case "quest":
            case "quests":
                handleQuest(player);
                break;
            case "war":
            case "wars":
                handleWar(player, args);
                break;
            case "perks":
            case "skills":
                handlePerks(player);
                break;
            case "logs":
                handleLogs(player);
                break;
            case "glow":
                handleGlow(player);
                break;
            case "reload":
                if (player.hasPermission("neonclans.admin")) {
                    plugin.reloadPlugin();
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Configuration and database reloaded!");
                } else {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to reload the plugin.");
                }
                break;
            case "help":
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan create <name> <tag>");
            return;
        }

        Clan existing = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (existing != null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are already in a clan!");
            return;
        }

        String name = args[1];
        String tag = args[2];

        // Validate lengths
        int minName = plugin.getConfig().getInt("creation.name-min-length", 3);
        int maxName = plugin.getConfig().getInt("creation.name-max-length", 16);
        int minTag = plugin.getConfig().getInt("creation.tag-min-length", 2);
        int maxTag = plugin.getConfig().getInt("creation.tag-max-length", 6);

        if (name.length() < minName || name.length() > maxName) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan name must be between " + minName + " and " + maxName + " characters.");
            return;
        }

        if (tag.length() < minTag || tag.length() > maxTag) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan tag must be between " + minTag + " and " + maxTag + " characters.");
            return;
        }

        if (plugin.getClanManager().getClanByName(name) != null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "A clan with that name already exists!");
            return;
        }

        if (plugin.getClanManager().getClanByTag(tag) != null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "A clan with that tag already exists!");
            return;
        }

        // Cost check
        boolean econEnabled = plugin.getConfig().getBoolean("creation.enabled", true);
        double cost = plugin.getConfig().getDouble("creation.cost", 100.0);

        if (econEnabled && plugin.getVaultHook().hasEconomy()) {
            if (!plugin.getVaultHook().has(player, cost)) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have enough money to create a clan! Cost: $" + cost);
                return;
            }
            plugin.getVaultHook().withdraw(player, cost);
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "$" + cost + " has been deducted for clan creation.");
        }

        Clan clan = plugin.getClanManager().createClan(name, tag, player.getUniqueId());
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Clan '" + clan.getName() + "' with tag '" + clan.getTag() + "' created successfully!");
    }

    private void handleDisband(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only the Clan Owner can disband the clan!");
            return;
        }

        // Broadcast to members
        String format = plugin.getPrefix() + ChatColor.RED + "The clan has been disbanded by the owner.";
        for (ClanMember m : clan.getMembers().values()) {
            Player online = Bukkit.getPlayer(m.getUuid());
            if (online != null && online.isOnline()) {
                online.sendMessage(format);
            }
        }

        plugin.getClanManager().disbandClan(clan.getId());
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan invite <player>");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        if (!clan.isOwner(player.getUniqueId()) && !clan.isModerator(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to invite players!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Player not found or offline.");
            return;
        }

        if (plugin.getClanManager().getClanByPlayer(target.getUniqueId()) != null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "That player is already in a clan.");
            return;
        }

        int maxMembers = plugin.getClanManager().getMaxMembers(clan.getLevel());
        if (clan.getMembers().size() >= maxMembers) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Your clan is full! Upgrade your clan level to increase capacity.");
            return;
        }

        plugin.getClanManager().invitePlayer(target.getUniqueId(), clan.getId());
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Invited " + target.getName() + " to join the clan.");
        target.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "You have been invited to join the clan " + ChatColor.YELLOW + clan.getName() + ChatColor.GREEN + ". Use /clan join " + clan.getName() + " to accept.");
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan join <clanName>");
            return;
        }

        Clan existing = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (existing != null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are already in a clan!");
            return;
        }

        String clanName = args[1];
        Clan clan = plugin.getClanManager().getClanByName(clanName);
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan not found.");
            return;
        }

        if (!plugin.getClanManager().hasInvite(player.getUniqueId(), clan.getId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have a pending invitation for this clan.");
            return;
        }

        int maxMembers = plugin.getClanManager().getMaxMembers(clan.getLevel());
        if (clan.getMembers().size() >= maxMembers) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "The clan is full.");
            return;
        }

        plugin.getClanManager().removeInvite(player.getUniqueId(), clan.getId());
        plugin.getClanManager().addMemberToClan(clan, player.getUniqueId());

        String broadcast = plugin.getPrefix() + ChatColor.YELLOW + player.getName() + ChatColor.GREEN + " has joined the clan!";
        for (ClanMember m : clan.getMembers().values()) {
            Player online = Bukkit.getPlayer(m.getUuid());
            if (online != null && online.isOnline()) {
                online.sendMessage(broadcast);
            }
        }
    }

    private void handleLeave(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        if (clan.isOwner(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "As the Owner, you cannot leave the clan. Disband it (/clan disband) or transfer ownership (/clan transfer <player>) first.");
            return;
        }

        plugin.getClanManager().removeMemberFromClan(clan, player.getUniqueId());
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "You left the clan.");

        String broadcast = plugin.getPrefix() + ChatColor.YELLOW + player.getName() + ChatColor.RED + " has left the clan.";
        for (ClanMember m : clan.getMembers().values()) {
            Player online = Bukkit.getPlayer(m.getUuid());
            if (online != null && online.isOnline()) {
                online.sendMessage(broadcast);
            }
        }
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan kick <player>");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        ClanMember selfMember = clan.getMember(player.getUniqueId());
        if (selfMember == null || selfMember.getRole() == ClanRole.MEMBER) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to kick members!");
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        ClanMember targetMember = clan.getMember(target.getUniqueId());

        if (targetMember == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "That player is not in your clan.");
            return;
        }

        if (targetMember.getUuid().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You cannot kick yourself!");
            return;
        }

        // Validate hierarchy
        if (selfMember.getRole() == ClanRole.MODERATOR && targetMember.getRole() != ClanRole.MEMBER) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Moderators can only kick normal members.");
            return;
        }

        plugin.getClanManager().removeMemberFromClan(clan, target.getUniqueId());
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Kicked " + target.getName() + " from the clan.");
        
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage(plugin.getPrefix() + ChatColor.RED + "You have been kicked from the clan " + clan.getName());
        }

        String broadcast = plugin.getPrefix() + ChatColor.YELLOW + target.getName() + ChatColor.RED + " was kicked by " + player.getName();
        for (ClanMember m : clan.getMembers().values()) {
            Player online = Bukkit.getPlayer(m.getUuid());
            if (online != null && online.isOnline()) {
                online.sendMessage(broadcast);
            }
        }
    }

    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan promote <player>");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only the Clan Owner can promote members!");
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        ClanMember targetMember = clan.getMember(target.getUniqueId());

        if (targetMember == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "That player is not in your clan.");
            return;
        }

        if (targetMember.getRole() != ClanRole.MEMBER) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "This member is already promoted.");
            return;
        }

        int mods = clan.getModeratorCount();
        int maxMods = plugin.getConfig().getInt("roles.moderator.max-promotions", 3);
        if (mods >= maxMods) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Moderator limit reached (" + maxMods + "). Demote someone first.");
            return;
        }

        plugin.getClanManager().promoteMember(clan, target.getUniqueId());
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Promoted " + target.getName() + " to Moderator.");

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "You have been promoted to Moderator in your clan!");
        }
    }

    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan demote <player>");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only the Clan Owner can demote moderators!");
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        ClanMember targetMember = clan.getMember(target.getUniqueId());

        if (targetMember == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "That player is not in your clan.");
            return;
        }

        if (targetMember.getRole() != ClanRole.MODERATOR) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "This member is not a moderator.");
            return;
        }

        plugin.getClanManager().demoteMember(clan, target.getUniqueId());
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Demoted " + target.getName() + " to Member.");

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage(plugin.getPrefix() + ChatColor.RED + "You have been demoted to Member in your clan.");
        }
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan transfer <player>");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only the Clan Owner can transfer ownership!");
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        ClanMember targetMember = clan.getMember(target.getUniqueId());

        if (targetMember == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "That player is not in your clan.");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are already the owner.");
            return;
        }

        plugin.getClanManager().transferOwnership(clan, target.getUniqueId());
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Transferred clan ownership to " + target.getName() + ". You are now a Moderator.");

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "You are now the Owner of the clan " + clan.getName() + "!");
        }
    }

    private void handleSetHome(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null || member.getRole() == ClanRole.MEMBER) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to set the clan home.");
            return;
        }

        plugin.getClanManager().setClanHome(clan, player.getLocation());
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Clan home location updated successfully!");
    }

    private void handleHome(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        Location home = clan.getHome();
        if (home == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Your clan has not set a home location yet! Set it with /clan sethome.");
            return;
        }

        // Check Cooldown
        UUID uuid = player.getUniqueId();
        long cooldownMs = plugin.getConfig().getLong("teleportation.cooldown-seconds", 10) * 1000L;
        long lastTeleport = teleportCooldowns.getOrDefault(uuid, 0L);
        long now = System.currentTimeMillis();

        if (now - lastTeleport < cooldownMs) {
            long remaining = (cooldownMs - (now - lastTeleport)) / 1000L;
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Teleport is on cooldown. Wait " + remaining + " seconds.");
            return;
        }

        // Handle Warmup
        int warmup = plugin.getConfig().getInt("teleportation.warmup-seconds", 3);
        if (warmup <= 0) {
            player.teleport(home);
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Teleported to clan home!");
            teleportCooldowns.put(uuid, now);
            return;
        }

        // Warmup delay
        if (activeTeleports.containsKey(uuid)) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are already teleporting!");
            return;
        }

        player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Teleporting in " + warmup + " seconds. Do not move!");
        Location startLoc = player.getLocation().clone();

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            activeTeleports.remove(uuid);
            Location current = player.getLocation();
            
            // Check if player moved blocks
            if (current.getBlockX() == startLoc.getBlockX() &&
                current.getBlockY() == startLoc.getBlockY() &&
                current.getBlockZ() == startLoc.getBlockZ()) {
                
                player.teleport(home);
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Teleported to clan home!");
                teleportCooldowns.put(uuid, System.currentTimeMillis());
            } else {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Teleport cancelled! You moved.");
            }
        }, warmup * 20L);

        activeTeleports.put(uuid, task);
    }

    private void handleFriendlyFire(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null || member.getRole() == ClanRole.MEMBER) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to toggle friendly fire.");
            return;
        }

        boolean current = clan.isFriendlyFireEnabled();
        plugin.getClanManager().setFriendlyFire(clan, !current);
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Friendly Fire toggled to: " + (!current ? "ENABLED" : "DISABLED"));
    }

    private void handleBank(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan bank <deposit/withdraw/balance> [amount]");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        String action = args[1].toLowerCase();
        
        if (action.equalsIgnoreCase("balance")) {
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Clan Bank Balance: " + ChatColor.YELLOW + "$" + String.format("%.2f", clan.getBalance()));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan bank " + action + " <amount>");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Please enter a valid positive number.");
            return;
        }

        if (action.equalsIgnoreCase("deposit")) {
            if (!plugin.getVaultHook().hasEconomy()) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Economy system is not available.");
                return;
            }

            if (!plugin.getVaultHook().has(player, amount)) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have enough money on you.");
                return;
            }

            plugin.getVaultHook().withdraw(player, amount);
            clan.deposit(amount);
            
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
                plugin.getDatabaseManager().saveClan(clan)
            );

            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Deposited $" + amount + " into the clan bank.");
            
            String broadcast = plugin.getPrefix() + ChatColor.YELLOW + player.getName() + ChatColor.GREEN + " deposited $" + amount + " into the clan bank.";
            for (ClanMember m : clan.getMembers().values()) {
                Player online = Bukkit.getPlayer(m.getUuid());
                if (online != null && online.isOnline() && !online.equals(player)) {
                    online.sendMessage(broadcast);
                }
            }

        } else if (action.equalsIgnoreCase("withdraw")) {
            ClanMember member = clan.getMember(player.getUniqueId());
            if (member == null || member.getRole() == ClanRole.MEMBER) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only owners and moderators can withdraw from the clan bank.");
                return;
            }

            if (!plugin.getVaultHook().hasEconomy()) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Economy system is not available.");
                return;
            }

            if (!clan.withdraw(amount)) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "The clan bank does not have enough funds.");
                return;
            }

            plugin.getVaultHook().deposit(player, amount);
            
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
                plugin.getDatabaseManager().saveClan(clan)
            );

            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Withdrew $" + amount + " from the clan bank.");
            
            String broadcast = plugin.getPrefix() + ChatColor.YELLOW + player.getName() + ChatColor.RED + " withdrew $" + amount + " from the clan bank.";
            for (ClanMember m : clan.getMembers().values()) {
                Player online = Bukkit.getPlayer(m.getUuid());
                if (online != null && online.isOnline() && !online.equals(player)) {
                    online.sendMessage(broadcast);
                }
            }
        }
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan ally <add/remove/chat> [clanName]");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        String sub = args[1].toLowerCase();

        if (sub.equalsIgnoreCase("chat")) {
            if (args.length < 3) {
                // Toggle ally chat
                ChatListener chatListener = plugin.getChatListener();
                if (chatListener != null) {
                    boolean enabled = chatListener.toggleAllyChat(player.getUniqueId());
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Ally chat toggled: " + (enabled ? "ENABLED" : "DISABLED"));
                }
                return;
            }

            // Send one-off ally message
            ChatListener chatListener = plugin.getChatListener();
            if (chatListener != null) {
                StringBuilder msg = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    msg.append(args[i]).append(" ");
                }
                chatListener.sendAllyMessage(player, clan, msg.toString().trim());
            }
            return;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan ally " + sub + " <clanName>");
            return;
        }

        ClanMember selfMember = clan.getMember(player.getUniqueId());
        if (selfMember == null || selfMember.getRole() == ClanRole.MEMBER) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not have permission to manage alliances.");
            return;
        }

        String targetName = args[2];
        Clan targetClan = plugin.getClanManager().getClanByName(targetName);
        if (targetClan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan not found.");
            return;
        }

        if (targetClan.getId().equals(clan.getId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You cannot ally with yourself.");
            return;
        }

        if (sub.equalsIgnoreCase("add")) {
            int maxAllies = plugin.getClanManager().getMaxAllies(clan.getLevel());
            if (clan.getAllies().size() >= maxAllies) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Alliance limit reached (" + maxAllies + "). Upgrade your level to ally with more clans.");
                return;
            }

            int targetMaxAllies = plugin.getClanManager().getMaxAllies(targetClan.getLevel());
            if (targetClan.getAllies().size() >= targetMaxAllies) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "That clan has reached their maximum alliances limit.");
                return;
            }

            // Check if there is an invite back or request
            // For simple ally request: if player allies target, and target has also allied player, they become allies!
            // If not, we register a temporary request (use pending invites as requests, keying invitee as targetClanId, inviter as clanId)
            plugin.getClanManager().invitePlayer(UUID.nameUUIDFromBytes(targetClan.getId().getBytes()), clan.getId()); // Using UUID hashed from clan ID as target key
            
            UUID targetHash = UUID.nameUUIDFromBytes(clan.getId().getBytes());
            if (plugin.getClanManager().hasInvite(UUID.nameUUIDFromBytes(clan.getId().getBytes()), targetClan.getId())) {
                // Both request each other, become allies!
                plugin.getClanManager().removeInvite(UUID.nameUUIDFromBytes(clan.getId().getBytes()), targetClan.getId());
                plugin.getClanManager().removeInvite(UUID.nameUUIDFromBytes(targetClan.getId().getBytes()), clan.getId());
                
                plugin.getClanManager().addAlliance(clan, targetClan);
                
                String broadcastSelf = plugin.getPrefix() + ChatColor.GREEN + "Alliance established with " + ChatColor.YELLOW + targetClan.getName() + "!";
                String broadcastTarget = plugin.getPrefix() + ChatColor.GREEN + "Alliance established with " + ChatColor.YELLOW + clan.getName() + "!";
                
                sendClanBroadcast(clan, broadcastSelf);
                sendClanBroadcast(targetClan, broadcastTarget);
            } else {
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Alliance request sent to " + targetClan.getName() + ".");
                // Alert owner/mods of target clan
                String requestMsg = plugin.getPrefix() + ChatColor.YELLOW + clan.getName() + ChatColor.GREEN + " has sent an alliance request. Use /clan ally add " + clan.getName() + " to accept!";
                sendClanStaffBroadcast(targetClan, requestMsg);
            }

        } else if (sub.equalsIgnoreCase("remove")) {
            if (!clan.isAlly(targetClan.getId())) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not allied with that clan.");
                return;
            }

            plugin.getClanManager().removeAlliance(clan, targetClan);
            
            String broadcastSelf = plugin.getPrefix() + ChatColor.RED + "Alliance with " + ChatColor.YELLOW + targetClan.getName() + ChatColor.RED + " has been dissolved.";
            String broadcastTarget = plugin.getPrefix() + ChatColor.RED + "Alliance with " + ChatColor.YELLOW + clan.getName() + ChatColor.RED + " has been dissolved.";
            
            sendClanBroadcast(clan, broadcastSelf);
            sendClanBroadcast(targetClan, broadcastTarget);
        }
    }

    private void handleTag(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only the Clan Owner can change the tag!");
            return;
        }

        if (!plugin.getClanManager().canCustomiseTag(clan)) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You must reach the maximum clan level to customize your tag layout/format!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan tag <newTag>");
            return;
        }

        String newTag = args[1];
        
        // Strip colors to check base length
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', newTag));
        int minTag = plugin.getConfig().getInt("creation.tag-min-length", 2);
        int maxTag = plugin.getConfig().getInt("creation.tag-max-length", 6);

        if (stripped.length() < minTag || stripped.length() > maxTag) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan tag must be between " + minTag + " and " + maxTag + " alphanumeric characters (excluding color codes).");
            return;
        }

        // Color and formatting check
        boolean colors = plugin.getConfig().getBoolean("max-level-custom-tag.allow-color-codes", true);
        boolean formatting = plugin.getConfig().getBoolean("max-level-custom-tag.allow-formatting-codes", true);

        if (!colors && newTag.contains("&")) {
            // Check if it's only colors
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Color codes are disabled for custom tags.");
            return;
        }

        // Apply customization
        clan.setTag(newTag);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> 
            plugin.getDatabaseManager().saveClan(clan)
        );

        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Clan tag successfully updated to: " + ChatColor.translateAlternateColorCodes('&', newTag));
    }

    private void handleInfo(Player player, String[] args) {
        Clan clan;
        if (args.length > 1) {
            clan = plugin.getClanManager().getClanByName(args[1]);
            if (clan == null) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan not found.");
                return;
            }
        } else {
            clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
            if (clan == null) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan! Use /clan info <clanName> to look up another clan.");
                return;
            }
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwnerUuid());
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m---------------------------------------"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d&lClan Info: &f&l" + clan.getName()));
        String tagColor = plugin.getClanManager().getClanTagColor(clan);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Tag: " + tagColor + clan.getTag()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Owner: &d" + (owner.getName() != null ? owner.getName() : "Unknown")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Level: &a" + clan.getLevel() + " &8/ &a" + plugin.getClanManager().getMaxLevel()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Bank Balance: &a$" + String.format("%.2f", clan.getBalance())));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Friendly Fire: " + (clan.isFriendlyFireEnabled() ? "&aEnabled" : "&cDisabled")));
        
        // Members list
        StringBuilder members = new StringBuilder();
        for (ClanMember m : clan.getMembers().values()) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(m.getUuid());
            String name = offline.getName() != null ? offline.getName() : "Unknown";
            String statusColor = offline.isOnline() ? "&a" : "&7";
            
            String roleSuffix = "";
            switch (m.getRole()) {
                case OWNER:
                    roleSuffix = plugin.getConfig().getString("roles.owner.suffix", "**");
                    break;
                case MODERATOR:
                    roleSuffix = plugin.getConfig().getString("roles.moderator.suffix", "*");
                    break;
                case MEMBER:
                    roleSuffix = plugin.getConfig().getString("roles.member.suffix", "");
                    break;
            }
            members.append(statusColor).append(name).append(roleSuffix).append("&7, ");
        }
        if (members.length() > 2) {
            members.setLength(members.length() - 2);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7Members (" + clan.getMembers().size() + "): " + members.toString()));

        // Allies list
        StringBuilder allies = new StringBuilder();
        for (String allyId : clan.getAllies()) {
            Clan ally = plugin.getClanManager().getClan(allyId);
            if (ally != null) {
                allies.append("&b").append(ally.getName()).append("&7, ");
            }
        }
        if (allies.length() > 2) {
            allies.setLength(allies.length() - 2);
        } else {
            allies.append("&7None");
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&bAllies: " + allies.toString()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m---------------------------------------"));
    }

    private void handleChat(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        ChatListener chatListener = plugin.getChatListener();
        if (chatListener == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Chat integration is currently unavailable.");
            return;
        }

        if (args.length < 2) {
            // Toggle chat
            boolean enabled = chatListener.toggleClanChat(player.getUniqueId());
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Clan chat toggled: " + (enabled ? "ENABLED" : "DISABLED"));
            return;
        }

        // Send message
        StringBuilder msg = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            msg.append(args[i]).append(" ");
        }
        chatListener.sendClanMessage(player, clan, msg.toString().trim());
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m---------------&r &d&lNeonClans Help &8&m---------------"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan &7- Opens interactive Clan Menu GUI."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan create <name> <tag> &7- Form a new clan (costs $100)."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan info [clanName] &7- Display statistics of a clan."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan invite <player> &7- Send alliance or membership invitation."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan join <clanName> &7- Accept a pending clan invitation."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan leave &7- Resign from your current clan."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan kick <player> &7- Evict a player from the clan."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan promote <player> &7- Promote member to moderator (* suffix)."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan demote <player> &7- Revert moderator to normal member."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan transfer <player> &7- Appoint a new clan Owner."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan sethome &7- Relocate the clan home coordinates."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan home &7- Teleport to the clan home."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan ff &7- Toggle friendly fire on/off."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan bank <deposit/withdraw/balance> [amt] &7- Manage bank."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan ally <add/remove/chat> [clan] &7- Manage alliances."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan tag <tag> &7- Change clan tag (Max level owner only)."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/clan chat [message] &7- Send message or toggle clan chat."));
        if (player.hasPermission("neonclans.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c/clan reload &7- Reload config files and database caches."));
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m---------------------------------------"));
    }



    private void sendClanBroadcast(Clan clan, String message) {
        for (ClanMember m : clan.getMembers().values()) {
            Player online = Bukkit.getPlayer(m.getUuid());
            if (online != null && online.isOnline()) {
                online.sendMessage(message);
            }
        }
    }

    private void sendClanStaffBroadcast(Clan clan, String message) {
        for (ClanMember m : clan.getMembers().values()) {
            if (m.getRole() == ClanRole.OWNER || m.getRole() == ClanRole.MODERATOR) {
                Player online = Bukkit.getPlayer(m.getUuid());
                if (online != null && online.isOnline()) {
                    online.sendMessage(message);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (!(sender instanceof Player)) return list;
        Player player = (Player) sender;

        if (args.length == 1) {
            String[] subs = {"menu", "create", "disband", "invite", "join", "leave", "kick", "promote", "demote", "transfer", "sethome", "home", "friendlyfire", "bank", "ally", "tag", "info", "chat", "top", "nexus", "quest", "war", "perks", "logs", "glow", "help"};
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) {
                    if (s.equals("reload") && !player.hasPermission("neonclans.admin")) continue;
                    list.add(s);
                }
            }
            return list;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
            
            if (sub.equals("kick") || sub.equals("promote") || sub.equals("demote") || sub.equals("transfer")) {
                if (clan != null) {
                    for (ClanMember m : clan.getMembers().values()) {
                        OfflinePlayer op = Bukkit.getOfflinePlayer(m.getUuid());
                        if (op.getName() != null && op.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            if (!op.getUniqueId().equals(player.getUniqueId())) {
                                list.add(op.getName());
                            }
                        }
                    }
                }
            } else if (sub.equals("invite")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(player) && p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        list.add(p.getName());
                    }
                }
            } else if (sub.equals("join") || sub.equals("info")) {
                for (Clan c : plugin.getClanManager().getClans().values()) {
                    if (c.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        list.add(c.getName());
                    }
                }
            } else if (sub.equals("bank")) {
                String[] bankActions = {"deposit", "withdraw", "balance"};
                for (String action : bankActions) {
                    if (action.startsWith(args[1].toLowerCase())) {
                        list.add(action);
                    }
                }
            } else if (sub.equals("ally")) {
                String[] allyActions = {"add", "remove", "chat"};
                for (String action : allyActions) {
                    if (action.startsWith(args[1].toLowerCase())) {
                        list.add(action);
                    }
                }
            }
            return list;
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            if (sub.equals("ally")) {
                if (action.equals("add") || action.equals("remove")) {
                    for (Clan c : plugin.getClanManager().getClans().values()) {
                        if (c.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            list.add(c.getName());
                        }
                    }
                }
            }
        }

        return list;
    }

    private void handleNexus(Player player, String[] args) {
        if (!plugin.getNexusManager().isEnabled()) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan Nexus feature is disabled.");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan nexus <place/remove/info>");
            return;
        }

        String action = args[1].toLowerCase();
        if (action.equals("place")) {
            ClanMember m = clan.getMember(player.getUniqueId());
            if (m == null || (m.getRole() != ClanRole.OWNER && m.getRole() != ClanRole.MODERATOR)) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only Owners/Moderators can set the Clan Nexus!");
                return;
            }

            plugin.getNexusManager().setNexus(clan, player.getLocation());
            plugin.getLogManager().log(clan.getId(), player.getName(), "NEXUS_SET", "Set nexus at " + player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ());
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Clan Nexus established at your location!");

        } else if (action.equals("remove")) {
            ClanMember m = clan.getMember(player.getUniqueId());
            if (m == null || (m.getRole() != ClanRole.OWNER && m.getRole() != ClanRole.MODERATOR)) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only Owners/Moderators can remove the Clan Nexus!");
                return;
            }

            plugin.getNexusManager().removeNexus(clan.getId());
            plugin.getLogManager().log(clan.getId(), player.getName(), "NEXUS_REMOVE", "Removed clan nexus");
            player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Clan Nexus removed.");

        } else if (action.equals("info")) {
            com.neonclans.model.ClanNexus nexus = plugin.getNexusManager().getNexus(clan.getId());
            if (nexus == null || nexus.getLocation() == null) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Your clan has not established a Nexus yet!");
            } else {
                double radius = plugin.getNexusManager().getNexusRadius(clan);
                Location l = nexus.getLocation();
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Nexus Location: " + ChatColor.YELLOW + l.getWorld().getName() + " (" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + ") &7| Radius: " + radius + "m");
            }
        }
    }

    private void handleQuest(Player player) {
        if (!plugin.getQuestManager().isEnabled()) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan Quests feature is disabled.");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        new ClanMenu(plugin).openQuestsMenu(player, clan);
    }

    private void handleWar(Player player, String[] args) {
        if (!plugin.getWarManager().isEnabled()) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan Wars feature is disabled.");
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan war <declare/accept/status/surrender> [clanName]");
            return;
        }

        String sub = args[1].toLowerCase();
        if (sub.equals("status")) {
            com.neonclans.model.ClanWar war = plugin.getWarManager().getActiveWar(clan.getId());
            if (war == null) {
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Your clan is not currently engaged in any active wars.");
            } else {
                Clan c1 = plugin.getClanManager().getClan(war.getClan1Id());
                Clan c2 = plugin.getClanManager().getClan(war.getClan2Id());
                String c1Name = c1 != null ? c1.getName() : "Unknown";
                String c2Name = c2 != null ? c2.getName() : "Unknown";
                long remainingSec = (war.getEndTime() - System.currentTimeMillis()) / 1000L;
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "&lWAR STATUS: &e" + c1Name + " (" + war.getClan1Kills() + ") &cVS &e" + c2Name + " (" + war.getClan2Kills() + ") &7| Time left: " + (remainingSec / 3600) + "h " + ((remainingSec % 3600) / 60) + "m");
            }
            return;
        }

        if (sub.equals("surrender")) {
            if (!clan.isOwner(player.getUniqueId())) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only the Clan Owner can surrender a war!");
                return;
            }
            com.neonclans.model.ClanWar war = plugin.getWarManager().getActiveWar(clan.getId());
            if (war == null) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Your clan is not in an active war.");
                return;
            }
            String winnerId = war.getClan1Id().equals(clan.getId()) ? war.getClan2Id() : war.getClan1Id();
            plugin.getWarManager().endWar(war, winnerId);
            plugin.getLogManager().log(clan.getId(), player.getName(), "WAR_SURRENDER", "Surrendered war to " + winnerId);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /clan war " + sub + " <targetClan>");
            return;
        }

        Clan target = plugin.getClanManager().getClanByName(args[2]);
        if (target == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Target clan not found.");
            return;
        }

        if (target.getId().equals(clan.getId())) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You cannot declare war on yourself.");
            return;
        }

        if (sub.equals("declare")) {
            if (!clan.isOwner(player.getUniqueId()) && !clan.isModerator(player.getUniqueId())) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only Owners/Moderators can declare war.");
                return;
            }
            plugin.getWarManager().declareWar(clan, target);
            plugin.getLogManager().log(clan.getId(), player.getName(), "WAR_DECLARE", "Declared war on " + target.getName());
            plugin.getDiscordWebhookManager().sendEmbed("⚔️ Clan War Declaration", clan.getName() + " has declared war on " + target.getName() + "!", 0xFF0000);
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "War declaration sent to " + target.getName() + ". They must use /clan war accept " + clan.getName() + " to begin!");

        } else if (sub.equals("accept")) {
            if (!clan.isOwner(player.getUniqueId()) && !clan.isModerator(player.getUniqueId())) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only Owners/Moderators can accept war declarations.");
                return;
            }
            if (!plugin.getWarManager().hasDeclaration(target, clan)) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "No pending war declaration from " + target.getName());
                return;
            }
            plugin.getWarManager().acceptWar(target, clan);
            plugin.getLogManager().log(clan.getId(), player.getName(), "WAR_ACCEPT", "Accepted war against " + target.getName());
            plugin.getDiscordWebhookManager().sendEmbed("⚔️ Clan War Begun!", target.getName() + " VS " + clan.getName() + " (24 Hour War)", 0xFF8800);
        }
    }

    private void handlePerks(Player player) {
        if (!plugin.getPerkManager().isEnabled()) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan Perks feature is disabled.");
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }
        new ClanMenu(plugin).openPerksMenu(player, clan);
    }

    private void handleLogs(Player player) {
        if (!plugin.getLogManager().isEnabled()) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Clan Audit Logs feature is disabled.");
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You are not in a clan!");
            return;
        }
        new ClanMenu(plugin).openAuditLogsMenu(player, clan);
    }

    private void handleGlow(Player player) {
        if (!plugin.getConfig().getBoolean("features.effects.enabled", true)) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Neon Visual Effects feature is disabled.");
            return;
        }
        boolean isGlowing = player.isGlowing();
        player.setGlowing(!isGlowing);
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Neon Glow toggled: " + (!isGlowing ? "ENABLED" : "DISABLED"));
    }
}
