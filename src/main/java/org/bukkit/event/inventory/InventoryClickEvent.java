package org.bukkit.event.inventory;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.InventoryView;

public class InventoryClickEvent {
    public InventoryView getView() {
        return null;
    }

    public int getRawSlot() {
        return 0;
    }

    public void setCancelled(boolean cancelled) {
    }

    public CommandSender getWhoClicked() {
        return null;
    }

    public ClickType getClick() {
        return ClickType.LEFT;
    }
}
