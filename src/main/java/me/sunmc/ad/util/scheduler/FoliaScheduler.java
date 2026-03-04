package me.sunmc.ad.util.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.sunmc.ad.AgriDeco;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Unified scheduler that works on both Folia and standard Paper.
 * <p>
 * On Folia  — delegates to RegionScheduler / AsyncScheduler / GlobalRegionScheduler.
 * On Paper  — delegates to BukkitScheduler (single-threaded, location context ignored).
 * <p>
 * All methods return a {@link TaskHandle} which wraps either a {@link ScheduledTask}
 * (Folia) or a {@link BukkitTask} (Paper) so callers can cancel without caring which
 * server software is running.
 */
public final class FoliaScheduler {

    /**
     * True when running on Folia (or any fork that ships RegionizedServer).
     */
    public static final boolean IS_FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        IS_FOLIA = folia;
    }

    /**
     * Runs {@code task} on the region that owns {@code loc}.
     * On Paper runs on the main thread (location is ignored).
     * If the chunk is unloaded on Folia, {@code retired} is called instead.
     */
    public void runAt(@NotNull Location loc, @NotNull Runnable task, @Nullable Runnable retired) {
        if (IS_FOLIA) {
            var world = loc.getWorld();
            if (world != null && !world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                if (retired != null) retired.run();
                return;
            }
            plugin.getServer().getRegionScheduler().run(plugin, loc, $ -> task.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    private final AgriDeco plugin;

    public FoliaScheduler(AgriDeco plugin) {
        this.plugin = plugin;
    }

    public void runAt(@NotNull Location loc, @NotNull Runnable task) {
        runAt(loc, task, null);
    }

    public void runAtDelayed(@NotNull Location loc, @NotNull Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            plugin.getServer().getRegionScheduler()
                    .runDelayed(plugin, loc, $ -> task.run(), delayTicks);
        } else {
            plugin.getServer().getScheduler()
                    .runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * Repeating task bound to a location/region.
     * Returns a {@link TaskHandle} — cancel it to stop the task.
     */
    public @NotNull TaskHandle runAtFixedRate(@NotNull Location loc, @NotNull Runnable task,
                                              long initialDelayTicks, long periodTicks) {
        if (IS_FOLIA) {
            ScheduledTask st = plugin.getServer().getRegionScheduler()
                    .runAtFixedRate(plugin, loc, $ -> task.run(), initialDelayTicks, periodTicks);
            return new TaskHandle(st);
        } else {
            BukkitTask bt = plugin.getServer().getScheduler()
                    .runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
            return new TaskHandle(bt);
        }
    }

    public void runGlobal(@NotNull Runnable task) {
        if (IS_FOLIA) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, $ -> task.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public @NotNull TaskHandle runGlobalFixedRate(@NotNull Runnable task,
                                                  long initialDelayTicks, long periodTicks) {
        if (IS_FOLIA) {
            ScheduledTask st = plugin.getServer().getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, $ -> task.run(), initialDelayTicks, periodTicks);
            return new TaskHandle(st);
        } else {
            BukkitTask bt = plugin.getServer().getScheduler()
                    .runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
            return new TaskHandle(bt);
        }
    }

    public void runAsync(@NotNull Runnable task) {
        if (IS_FOLIA) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, $ -> task.run());
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void runAsyncDelayed(@NotNull Runnable task, long delay, @NotNull TimeUnit unit) {
        if (IS_FOLIA) {
            plugin.getServer().getAsyncScheduler()
                    .runDelayed(plugin, $ -> task.run(), delay, unit);
        } else {
            long ticks = unit.toMillis(delay) / 50; // 1 tick = 50 ms
            plugin.getServer().getScheduler()
                    .runTaskLaterAsynchronously(plugin, task, ticks);
        }
    }

    public @NotNull TaskHandle runAsyncFixedRate(@NotNull Runnable task,
                                                 long initialDelay, long period,
                                                 @NotNull TimeUnit unit) {
        if (IS_FOLIA) {
            ScheduledTask st = plugin.getServer().getAsyncScheduler()
                    .runAtFixedRate(plugin, $ -> task.run(), initialDelay, period, unit);
            return new TaskHandle(st);
        } else {
            long initTicks = unit.toMillis(initialDelay) / 50;
            long periodTicks = unit.toMillis(period) / 50;
            BukkitTask bt = plugin.getServer().getScheduler()
                    .runTaskTimerAsynchronously(plugin, task, initTicks, periodTicks);
            return new TaskHandle(bt);
        }
    }

    public void runOnEntity(@NotNull Entity entity, @NotNull Runnable task,
                            @Nullable Runnable retired) {
        if (IS_FOLIA) {
            entity.getScheduler().run(plugin, $ -> task.run(), retired);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Thin wrapper so callers can cancel tasks regardless of server type.
     */
    public static final class TaskHandle {
        private final Object inner; // ScheduledTask | BukkitTask

        private TaskHandle(Object inner) {
            this.inner = inner;
        }

        public void cancel() {
            if (inner instanceof ScheduledTask st) st.cancel();
            else if (inner instanceof BukkitTask bt) bt.cancel();
        }
    }
}