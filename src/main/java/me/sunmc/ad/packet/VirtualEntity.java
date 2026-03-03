package me.sunmc.ad.packet;

import java.util.concurrent.atomic.AtomicInteger;

public final class VirtualEntity {

    private static final AtomicInteger COUNTER = new AtomicInteger(Integer.MAX_VALUE / 2);

    private VirtualEntity() {
    }

    public static int nextId() {
        return COUNTER.getAndDecrement();
    }
}