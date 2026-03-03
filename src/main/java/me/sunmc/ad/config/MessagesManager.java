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
    private volatile Map<String, Component> messages = Map.of();
    private volatile Map<String, String> rawMessages = Map.of();

    public MessagesManager(@NotNull AgriDeco plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        reload();
    }

    public void reload() {
        var cfg = YamlConfiguration.loadConfiguration(file);
        var sec = cfg.getConfigurationSection("messages");
        var components = new HashMap<String, Component>();
        var raws = new HashMap<String, String>();
        if (sec != null) {
            sec.getKeys(false).forEach(k -> {
                String raw = sec.getString(k, "");
                raws.put(k, raw);
                components.put(k, ColorUtil.component(raw));
            });
        }
        messages = Map.copyOf(components);
        rawMessages = Map.copyOf(raws);
    }

    public @NotNull Component get(String key) {
        return messages.getOrDefault(key, ColorUtil.component("<red>[Missing message: " + key + "]"));
    }

    /**
     * Sends a message with {placeholder} replacements — no disk read.
     */
    public void send(@NotNull CommandSender sender, String key, String @NotNull ... pairs) {
        if (pairs.length == 0) {
            sender.sendMessage(get(key));
            return;
        }
        var raw = getRaw(key);
        for (int i = 0; i + 1 < pairs.length; i += 2)
            raw = raw.replace(pairs[i], pairs[i + 1]);
        sender.sendMessage(ColorUtil.component(raw));
    }

    public void send(@NotNull CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    private String getRaw(String key) {
        return rawMessages.getOrDefault(key, "<red>[Missing message: " + key + "]");
    }
}