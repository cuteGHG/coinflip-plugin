package org.bukkit.scheduler;

import org.bukkit.plugin.java.JavaPlugin;

public interface BukkitScheduler {
    void runTask(JavaPlugin plugin, Runnable task);
}
