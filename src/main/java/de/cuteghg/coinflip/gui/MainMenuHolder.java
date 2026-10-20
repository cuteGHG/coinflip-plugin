package de.cuteghg.coinflip.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MainMenuHolder implements InventoryHolder {
    private final int page;

    public MainMenuHolder(int page) {
        this.page = page;
    }

    public int page() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
