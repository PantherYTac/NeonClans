package com.neonclans.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class ClanMenuHolder implements InventoryHolder {
    private Inventory inventory;
    private final MenuClickHandler clickHandler;

    public ClanMenuHolder(MenuClickHandler clickHandler) {
        this.clickHandler = clickHandler;
    }

    public MenuClickHandler getClickHandler() {
        return clickHandler;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public interface MenuClickHandler {
        void onClick(InventoryClickEvent event);
    }
}
