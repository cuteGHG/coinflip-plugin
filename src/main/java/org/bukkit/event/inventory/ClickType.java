package org.bukkit.event.inventory;

public enum ClickType {
    LEFT,
    RIGHT;

    public boolean isRightClick() {
        return this == RIGHT;
    }
}
