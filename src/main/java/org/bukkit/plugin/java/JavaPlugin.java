package org.bukkit.plugin.java;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.logging.Logger;

public class JavaPlugin implements CommandExecutor {
    public void onEnable() {
    }

    public void onDisable() {
    }

    public PluginCommand getCommand(String name) {
        return null;
    }

    public void saveDefaultConfig() {
    }

    public FileConfiguration getConfig() {
        return new FileConfiguration();
    }

    public File getDataFolder() {
        return new File(".");
    }

    public Logger getLogger() {
        return Logger.getLogger("JavaPlugin");
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        return false;
    }
}
