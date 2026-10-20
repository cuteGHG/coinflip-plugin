package org.bukkit.inventory;

public interface Inventory {
    InventoryHolder getHolder();

    int getSize();

    ItemStack getItem(int slot);

    void setItem(int slot, ItemStack stack);
}
