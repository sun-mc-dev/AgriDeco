package me.sunmc.ad.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class PlacedCrop {

    private final UUID uuid;
    private final String configId;
    private final String worldName;
    private final double x, y, z;
    private final UUID ownerUuid;
    private final int virtualEntityId;
    private final AtomicInteger stage = new AtomicInteger(0);

    public PlacedCrop(UUID uuid, String configId, @NotNull Location location,
                      UUID ownerUuid, int virtualEntityId) {
        this.uuid = uuid;
        this.configId = configId;
        this.worldName = location.getWorld() != null ? location.getWorld().getName() : "";
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.ownerUuid = ownerUuid;
        this.virtualEntityId = virtualEntityId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getConfigId() {
        return configId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public int getVirtualEntityId() {
        return virtualEntityId;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public int getStage() {
        return stage.get();
    }

    public void setStage(int v) {
        stage.set(v);
    }

    public boolean tryAdvanceStage(int expected, int next) {
        return stage.compareAndSet(expected, next);
    }

    /**
     * Constructs a new Location each call — use sparingly in hot paths.
     */
    public @NotNull Location getLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }
}