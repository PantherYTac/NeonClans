package com.neonclans.model;

public class ClanPerk {
    private final String clanId;
    private final String perkId;
    private int level;

    public ClanPerk(String clanId, String perkId, int level) {
        this.clanId = clanId;
        this.perkId = perkId;
        this.level = level;
    }

    public String getClanId() {
        return clanId;
    }

    public String getPerkId() {
        return perkId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
