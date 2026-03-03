package me.sunmc.ad.listener;

import me.sunmc.ad.AgriDeco;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

public final class BlockBreakListener implements Listener {

    private final AgriDeco plugin;

    public BlockBreakListener(AgriDeco plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(@NotNull BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.BARRIER) return;
        var fm = plugin.getFurnitureManager();
        if (fm != null && fm.isFurnitureBarrier(e.getBlock().getLocation()))
            e.setCancelled(true);
    }
}
