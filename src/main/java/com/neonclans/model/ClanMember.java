package com.neonclans.model;

import java.util.UUID;

public class ClanMember {
    private final UUID uuid;
    private final String clanId;
    private ClanRole role;
    private final long joinedAt;

    public ClanMember(UUID uuid, String clanId, ClanRole role, long joinedAt) {
        this.uuid = uuid;
        this.clanId = clanId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getClanId() {
        return clanId;
    }

    public ClanRole getRole() {
        return role;
    }

    public void setRole(ClanRole role) {
        this.role = role;
    }

    public long getJoinedAt() {
        return joinedAt;
    }
}
