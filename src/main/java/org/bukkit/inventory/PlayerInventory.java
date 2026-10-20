package org.bukkit.inventory;

import java.util.Map;

public interface PlayerInventory extends Inventory {
    ItemStack[] getContents();

    void setContents(ItemStack[] contents);

    Map<Integer, ItemStack> addItem(ItemStack... stacks);
}
