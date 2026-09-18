package com.neonclans.model;

public class ClanWar {
    private final String warId;
    private final String clan1Id;
    private final String clan2Id;
    private int clan1Kills;
    private int clan2Kills;
    private final long startTime;
    private final long endTime;
    private String status; // ACTIVE, CLAN1_WON, CLAN2_WON, DRAW

    public ClanWar(String warId, String clan1Id, String clan2Id, int clan1Kills, int clan2Kills, long startTime, long endTime, String status) {
        this.warId = warId;
        this.clan1Id = clan1Id;
        this.clan2Id = clan2Id;
        this.clan1Kills = clan1Kills;
        this.clan2Kills = clan2Kills;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public String getWarId() {
        return warId;
    }

    public String getClan1Id() {
        return clan1Id;
    }

    public String getClan2Id() {
        return clan2Id;
    }

    public int getClan1Kills() {
        return clan1Kills;
    }

    public void incrementClan1Kills() {
        this.clan1Kills++;
    }

    public int getClan2Kills() {
        return clan2Kills;
    }

    public void incrementClan2Kills() {
        this.clan2Kills++;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= endTime && status.equalsIgnoreCase("ACTIVE");
    }
}
