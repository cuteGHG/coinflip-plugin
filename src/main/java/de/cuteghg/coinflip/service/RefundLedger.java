package de.cuteghg.coinflip.service;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class RefundLedger {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Integer> pendingRefunds = new HashMap<>();

    public RefundLedger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "refunds.db");
    }

    public void load() {
        pendingRefunds.clear();
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String[] parts = trimmed.split(":", 2);
                if (parts.length != 2) {
                    continue;
                }

                try {
                    UUID playerId = UUID.fromString(parts[0]);
                    int amount = Integer.parseInt(parts[1]);
                    if (amount > 0) {
                        pendingRefunds.put(playerId, amount);
                    }
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid refund entry: " + trimmed);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load refunds.db", e);
        }
    }

    public synchronized int add(UUID playerId, int amount) {
        if (amount <= 0) {
            return pendingRefunds.getOrDefault(playerId, 0);
        }
        int updated = pendingRefunds.getOrDefault(playerId, 0) + amount;
        pendingRefunds.put(playerId, updated);
        save();
        return updated;
    }

    public synchronized int consume(UUID playerId) {
        Integer current = pendingRefunds.remove(playerId);
        save();
        return current == null ? 0 : current;
    }

    public synchronized void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Data folder could not be created for refunds.db");
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (Map.Entry<UUID, Integer> entry : pendingRefunds.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save refunds.db", e);
        }
    }
}
