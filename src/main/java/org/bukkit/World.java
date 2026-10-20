package org.bukkit;

import org.bukkit.inventory.ItemStack;

public interface World {
    void dropItemNaturally(Location location, ItemStack itemStack);
}
