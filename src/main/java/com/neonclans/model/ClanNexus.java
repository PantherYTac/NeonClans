package com.neonclans.model;

import org.bukkit.Location;

public class ClanNexus {
    private final String clanId;
    private Location location;

    public ClanNexus(String clanId, Location location) {
        this.clanId = clanId;
        this.location = location;
    }

    public String getClanId() {
        return clanId;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
