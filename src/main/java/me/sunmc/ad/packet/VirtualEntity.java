package me.sunmc.ad.packet;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Issues virtual entity IDs that are guaranteed not to collide with server-side IDs.
 * <p>
 * Server entity IDs count up from 1. We use the upper half of the positive int range
 * (starting at 0x4000_0000 = 1 073 741 824) and count upward with wrap-around
 * back to START, keeping us safely away from low server-assigned IDs.
 * <p>
 * Modular counter with explicit safe range instead of unbounded decrement.
 */
public final class VirtualEntity {

    // Start just above 1 billion; end before Integer.MAX_VALUE to leave a buffer.
    private static final int START = 0x4000_0000;           // 1 073 741 824
    private static final int END = Integer.MAX_VALUE - 1; // 2 147 483 646

    private static final AtomicInteger COUNTER = new AtomicInteger(START);

    private VirtualEntity() {
    }

    public static int nextId() {
        return COUNTER.updateAndGet(v -> v >= END ? START : v + 1);
    }
}