package me.sunmc.ad.listener;

import me.sunmc.ad.AgriDeco;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.jetbrains.annotations.NotNull;

public final class ChunkLoadListener implements Listener {

    private final AgriDeco plugin;

    public ChunkLoadListener(AgriDeco plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent e) {
        plugin.getFoliaScheduler().runAtDelayed(
                e.getPlayer().getLocation(),
                () -> plugin.getPacketHandler().sendAllEntitiesToPlayer(e.getPlayer()),
                20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(@NotNull ChunkLoadEvent e) {
        for (var entity : e.getChunk().getEntities()) {
            if (!(entity instanceof Player p)) continue;
            plugin.getFoliaScheduler().runAtDelayed(
                    p.getLocation(),
                    () -> plugin.getPacketHandler().sendAllEntitiesToPlayer(p),
                    5L);
        }
    }
}