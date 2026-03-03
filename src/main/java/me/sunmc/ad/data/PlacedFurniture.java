package me.sunmc.ad.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class PlacedFurniture {

    private final UUID uuid;
    private final String configId;
    private final String worldName;
    private final double x, y, z;
    private final float yaw;
    private final UUID ownerUuid;
    private final int virtualEntityId;
    private final AtomicInteger interactionState = new AtomicInteger(0);

    public PlacedFurniture(UUID uuid, String configId, @NotNull Location location,
                           UUID ownerUuid, int virtualEntityId) {
        this.uuid = uuid;
        this.configId = configId;
        this.worldName = location.getWorld() != null ? location.getWorld().getName() : "";
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
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

    public float getYaw() {
        return yaw;
    }

    public int getInteractionState() {
        return interactionState.get();
    }

    public void setInteractionState(int v) {
        interactionState.set(v);
    }

    /**
     * Constructs a new Location each call — use sparingly in hot paths.
     */
    public @NotNull Location getLocation() {
        var world = Bukkit.getWorld(worldName);
        return new Location(world, x, y, z, yaw, 0f);
    }
}