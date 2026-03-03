package me.sunmc.ad.listener;

import me.sunmc.ad.AgriDeco;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerQuitListener implements Listener {

    private final AgriDeco plugin;

    public PlayerQuitListener(AgriDeco plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent e) {
        Entity vehicle = e.getPlayer().getVehicle();
        if (!(vehicle instanceof ArmorStand stand)) return;
        var fm = plugin.getFurnitureManager();
        if (fm == null || !fm.isSeatStand(stand.getUniqueId())) return;

        plugin.getFoliaScheduler().runOnEntity(stand, () -> {
            stand.eject();
            stand.remove();
            fm.removeSeatStand(stand.getUniqueId());
        }, () -> fm.removeSeatStand(stand.getUniqueId()));
    }
}