package org.bukkit.entity;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public interface Player extends CommandSender, OfflinePlayer {
    boolean hasPermission(String permission);

    UUID getUniqueId();

    void openInventory(Inventory inventory);

    void closeInventory();

    PlayerInventory getInventory();

    void updateInventory();

    World getWorld();

    Location getLocation();

    boolean isOnline();
}
