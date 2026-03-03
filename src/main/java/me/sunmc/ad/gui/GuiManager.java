package me.sunmc.ad.gui;

import me.sunmc.ad.AgriDeco;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiManager {

    private final AgriDeco plugin;
    private final Map<UUID, BaseGui> open = new ConcurrentHashMap<>();

    public GuiManager(AgriDeco plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        var gui = new MainMenuGui(plugin);
        gui.open(player);
        open.put(player.getUniqueId(), gui);
    }

    public void openFurniture(Player player, int page) {
        var gui = new FurnitureListGui(plugin, page);
        gui.open(player);
        open.put(player.getUniqueId(), gui);
    }

    public void openCrops(Player player, int page) {
        var gui = new CropListGui(plugin, page);
        gui.open(player);
        open.put(player.getUniqueId(), gui);
    }

    public BaseGui get(@NotNull Player player) {
        return open.get(player.getUniqueId());
    }

    public void remove(@NotNull Player player) {
        open.remove(player.getUniqueId());
    }
}
