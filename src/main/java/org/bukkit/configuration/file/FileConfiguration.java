package org.bukkit.configuration.file;

import java.util.Collections;
import java.util.List;

public class FileConfiguration {
    public int getInt(String path, int def) {
        return def;
    }

    public String getString(String path, String def) {
        return def;
    }

    public List<Integer> getIntegerList(String path) {
        return Collections.emptyList();
    }
}
