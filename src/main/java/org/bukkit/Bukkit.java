package org.bukkit;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public final class Bukkit {
    private Bukkit() {
    }

    public static PluginManager getPluginManager() {
        return null;
    }

    public static Collection<? extends org.bukkit.entity.Player> getOnlinePlayers() {
        return Collections.emptyList();
    }

    public static org.bukkit.entity.Player getPlayer(UUID uuid) {
        return null;
    }

    public static OfflinePlayer getOfflinePlayer(UUID uuid) {
        return null;
    }

    public static BukkitScheduler getScheduler() {
        return null;
    }

    public static Inventory createInventory(InventoryHolder holder, int size, String title) {
        return null;
    }
}
