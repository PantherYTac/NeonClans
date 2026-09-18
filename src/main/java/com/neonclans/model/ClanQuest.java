package com.neonclans.model;

public class ClanQuest {
    private final String clanId;
    private final String questType; // MINE_BLOCKS, KILL_MOBS, PVP_KILLS
    private final int targetAmount;
    private int currentProgress;
    private boolean completed;

    public ClanQuest(String clanId, String questType, int targetAmount, int currentProgress, boolean completed) {
        this.clanId = clanId;
        this.questType = questType;
        this.targetAmount = targetAmount;
        this.currentProgress = currentProgress;
        this.completed = completed;
    }

    public String getClanId() {
        return clanId;
    }

    public String getQuestType() {
        return questType;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void incrementProgress(int amount) {
        if (completed) return;
        this.currentProgress += amount;
        if (this.currentProgress >= this.targetAmount) {
            this.currentProgress = this.targetAmount;
            this.completed = true;
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
