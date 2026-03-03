package me.sunmc.ad;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {

    public static NamespacedKey FURNITURE_ID;
    public static NamespacedKey CROP_ID;

    private Keys() {
    }

    public static void init(Plugin plugin) {
        FURNITURE_ID = new NamespacedKey(plugin, "furniture_id");
        CROP_ID = new NamespacedKey(plugin, "crop_id");
    }
}