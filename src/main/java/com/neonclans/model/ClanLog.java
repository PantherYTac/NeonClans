package com.neonclans.model;

public class ClanLog {
    private final int id;
    private final String clanId;
    private final String playerName;
    private final String actionType;
    private final String details;
    private final long timestamp;

    public ClanLog(int id, String clanId, String playerName, String actionType, String details, long timestamp) {
        this.id = id;
        this.clanId = clanId;
        this.playerName = playerName;
        this.actionType = actionType;
        this.details = details;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public String getClanId() {
        return clanId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getDetails() {
        return details;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
