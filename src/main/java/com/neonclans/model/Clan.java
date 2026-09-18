package com.neonclans.model;

import org.bukkit.Location;
import java.util.*;

public class Clan {
    private final String id;
    private String name;
    private String tag;
    private UUID ownerUuid;
    private int level;
    private double balance;
    private Location home;
    private boolean friendlyFire;
    private final long createdAt;
    
    private final Map<UUID, ClanMember> members = new HashMap<>();
    private final Set<String> allies = new HashSet<>();
    private String vaultData = ""; // Base64 serialized Inventory data

    public Clan(String id, String name, String tag, UUID ownerUuid, int level, double balance, Location home, boolean friendlyFire, long createdAt) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerUuid = ownerUuid;
        this.level = level;
        this.balance = balance;
        this.home = home;
        this.friendlyFire = friendlyFire;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void deposit(double amount) {
        this.balance += amount;
    }

    public boolean withdraw(double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            return true;
        }
        return false;
    }

    public Location getHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public boolean isFriendlyFireEnabled() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Members
    public Map<UUID, ClanMember> getMembers() {
        return members;
    }

    public void addMember(ClanMember member) {
        members.put(member.getUuid(), member);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public ClanMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public boolean isOwner(UUID uuid) {
        return ownerUuid.equals(uuid);
    }

    public boolean isModerator(UUID uuid) {
        ClanMember member = members.get(uuid);
        return member != null && member.getRole() == ClanRole.MODERATOR;
    }

    public int getModeratorCount() {
        int count = 0;
        for (ClanMember member : members.values()) {
            if (member.getRole() == ClanRole.MODERATOR) {
                count++;
            }
        }
        return count;
    }

    // Allies
    public Set<String> getAllies() {
        return allies;
    }

    public void addAlly(String clanId) {
        allies.add(clanId);
    }

    public void removeAlly(String clanId) {
        allies.remove(clanId);
    }

    public boolean isAlly(String clanId) {
        return allies.contains(clanId);
    }

    // Vault Data
    public String getVaultData() {
        return vaultData;
    }

    public void setVaultData(String vaultData) {
        this.vaultData = vaultData;
    }
}
