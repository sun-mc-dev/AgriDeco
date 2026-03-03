package me.sunmc.ad.util;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Canonical key utilities for chunk-bucketed and block-exact lookups.
 * <p>
 * Chunk key  — same formula Minecraft uses internally (matches Chunk.getChunkKey()).
 * bits: [63..32] = cz (signed 32-bit), [31..0] = cx (signed 32-bit)
 * Safe for the full MC coordinate range ±30 000 000.
 * <p>
 * Block key  — packs (x, y, z) into one long using Minecraft's own BlockPos encoding:
 * bits: [63..38] = x (26-bit signed), [37..26] = y (12-bit signed), [25..0] = z (26-bit signed)
 * Valid for x/z ∈ [-33 554 432, 33 554 431] and y ∈ [-2048, 2047] — covers all Minecraft limits.
 * The old formula used only 24 bits for x/z, causing collisions near the world border.
 */
public final class ChunkKey {

    private ChunkKey() {
    }

    /**
     * Chunk-granularity key, safe across the full MC coordinate space.
     */
    public static long of(@NotNull Location loc) {
        long cx = loc.getBlockX() >> 4;
        long cz = loc.getBlockZ() >> 4;
        // Match Bukkit's Chunk.getChunkKey() layout
        return (cz & 0xFFFFFFFFL) << 32 | (cx & 0xFFFFFFFFL);
    }

    /**
     * Chunk key from raw chunk coordinates (used by sendVisible loops).
     */
    public static long of(int cx, int cz) {
        return ((long) cz & 0xFFFFFFFFL) << 32 | ((long) cx & 0xFFFFFFFFL);
    }

    /**
     * Block-exact key. Replaces the old FurnitureManager.blockKey().
     */
    public static long block(@NotNull Location loc) {
        long x = loc.getBlockX();
        long y = loc.getBlockY();
        long z = loc.getBlockZ();
        return ((x & 0x3FFFFFFL) << 38) | ((y & 0xFFFL) << 26) | (z & 0x3FFFFFFL);
    }
}