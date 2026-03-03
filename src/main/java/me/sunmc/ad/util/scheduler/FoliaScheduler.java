package me.sunmc.ad.util.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.sunmc.ad.AgriDeco;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class FoliaScheduler {

    private final AgriDeco plugin;

    public FoliaScheduler(AgriDeco plugin) {
        this.plugin = plugin;
    }

    public void runAt(Location loc, Runnable task) {
        plugin.getServer().getRegionScheduler().run(plugin, loc, $ -> task.run());
    }

    public void runGlobal(Runnable task) {
        plugin.getServer().getGlobalRegionScheduler().run(plugin, $ -> task.run());
    }

    public void runAsync(Runnable task) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, $ -> task.run());
    }

    public void runOnEntity(@NotNull Entity entity, Runnable task, Runnable retired) {
        entity.getScheduler().run(plugin, $ -> task.run(), retired);
    }

    public void runAtDelayed(Location loc, Runnable task, long delayTicks) {
        plugin.getServer().getRegionScheduler()
                .runDelayed(plugin, loc, $ -> task.run(), delayTicks);
    }

    public void runAsyncDelayed(Runnable task, long delay, TimeUnit unit) {
        plugin.getServer().getAsyncScheduler()
                .runDelayed(plugin, $ -> task.run(), delay, unit);
    }

    public @NotNull ScheduledTask runAtFixedRate(Location loc, Runnable task,
                                                 long initialDelayTicks, long periodTicks) {
        return plugin.getServer().getRegionScheduler()
                .runAtFixedRate(plugin, loc, $ -> task.run(), initialDelayTicks, periodTicks);
    }

    public @NotNull ScheduledTask runAsyncFixedRate(Runnable task, long initialDelay,
                                                    long period, TimeUnit unit) {
        return plugin.getServer().getAsyncScheduler()
                .runAtFixedRate(plugin, $ -> task.run(), initialDelay, period, unit);
    }

    public @NotNull ScheduledTask runGlobalFixedRate(Runnable task,
                                                     long initialDelayTicks, long periodTicks) {
        return plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, $ -> task.run(), initialDelayTicks, periodTicks);
    }
}