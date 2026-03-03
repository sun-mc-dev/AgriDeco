package me.sunmc.ad.integration.hook;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.sunmc.ad.AgriDeco;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class GriefPreventionHook {

    private final boolean enabled;

    public GriefPreventionHook(AgriDeco plugin, boolean enabled) {
        this.enabled = enabled;
        if (enabled) plugin.getSLF4JLogger().info("GriefPrevention hooked.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean canBuild(Player player, Location loc) {
        if (!enabled) return true;
        try {
            return GriefPrevention.instance.allowBuild(player, loc) == null;
        } catch (Exception e) {
            return true;
        }
    }
}