package me.sunmc.ad.config;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.util.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class MessagesManager {

    private final AgriDeco plugin;
    private final File file;
    private volatile Map<String, String> messages = new HashMap<>();

    public MessagesManager(@NotNull AgriDeco plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        reload();
    }

    public void reload() {
        var cfg = YamlConfiguration.loadConfiguration(file);
        var sec = cfg.getConfigurationSection("messages");
        var map = new HashMap<String, String>();
        if (sec != null)
            sec.getKeys(false).forEach(k ->
                    map.put(k, ColorUtil.translate(sec.getString(k, ""))));
        messages = Map.copyOf(map);
    }

    public String get(String key) {
        return messages.getOrDefault(key, "§c[Missing: " + key + "]");
    }

    public void send(@NotNull CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    public void send(CommandSender sender, String key, String @NotNull ... pairs) {
        String msg = get(key);
        for (int i = 0; i + 1 < pairs.length; i += 2)
            msg = msg.replace(pairs[i], pairs[i + 1]);
        sender.sendMessage(msg);
    }
}