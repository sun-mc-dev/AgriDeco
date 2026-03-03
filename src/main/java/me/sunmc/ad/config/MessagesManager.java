package me.sunmc.ad.config;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class MessagesManager {

    private final AgriDeco plugin;
    private final File file;
    // Store as Component — deserialized once on load/reload, sent directly (no deprecated String overload)
    private volatile Map<String, Component> messages = Map.of();

    public MessagesManager(@NotNull AgriDeco plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        reload();
    }

    public void reload() {
        var cfg = YamlConfiguration.loadConfiguration(file);
        var sec = cfg.getConfigurationSection("messages");
        var map = new HashMap<String, Component>();
        if (sec != null)
            sec.getKeys(false).forEach(k ->
                    map.put(k, ColorUtil.component(sec.getString(k, ""))));
        messages = Map.copyOf(map);
    }

    /**
     * Returns the raw Component for a key, or a red error placeholder.
     */
    public @NotNull Component get(String key) {
        return messages.getOrDefault(key,
                ColorUtil.component("<red>[Missing: " + key + "]"));
    }

    /**
     * Sends a message, replacing {placeholder} tokens before deserialization.
     * pairs: alternating placeholder, value — e.g. "{type}", "furniture", "{id}", "chair"
     */
    public void send(@NotNull CommandSender sender, String key, String @NotNull ... pairs) {
        if (pairs.length == 0) {
            sender.sendMessage(get(key));
            return;
        }
        // Re-deserialize with replacements so MiniMessage tags inside values still work
        var raw = getRaw(key);
        for (int i = 0; i + 1 < pairs.length; i += 2)
            raw = raw.replace(pairs[i], pairs[i + 1]);
        sender.sendMessage(ColorUtil.component(raw));
    }

    public void send(@NotNull CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    /**
     * Returns the raw MiniMessage string for a key (needed for placeholder substitution).
     */
    private String getRaw(String key) {
        var cfg = YamlConfiguration.loadConfiguration(file);
        var sec = cfg.getConfigurationSection("messages");
        return sec != null ? sec.getString(key, "<red>[Missing: " + key + "]") : "<red>[Missing: " + key + "]";
    }
}