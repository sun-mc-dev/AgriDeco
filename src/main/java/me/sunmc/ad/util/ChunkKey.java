package me.sunmc.ad.util;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public final class ChunkKey {

    private ChunkKey() {
    }

    public static long of(@NotNull Location loc) {
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;
        return ((long) cx & 0xFFFFFFFFL) | (((long) cz & 0xFFFFFFFFL) << 32);
    }
}
