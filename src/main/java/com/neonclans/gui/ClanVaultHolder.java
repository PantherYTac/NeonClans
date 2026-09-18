package com.neonclans.gui;

import com.neonclans.model.Clan;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ClanVaultHolder implements InventoryHolder {
    private final Clan clan;
    private Inventory inventory;

    public ClanVaultHolder(Clan clan) {
        this.clan = clan;
    }

    public Clan getClan() {
        return clan;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
