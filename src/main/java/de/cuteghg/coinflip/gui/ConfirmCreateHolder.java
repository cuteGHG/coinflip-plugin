package de.cuteghg.coinflip.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ConfirmCreateHolder implements InventoryHolder {
    private final int stake;

    public ConfirmCreateHolder(int stake) {
        this.stake = stake;
    }

    public int stake() {
        return stake;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
