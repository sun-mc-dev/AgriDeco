package me.sunmc.ad.data;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class PlacedFurniture {

    private final UUID uuid;
    private final String configId;
    private final Location location;
    private final UUID ownerUuid;
    private final int virtualEntityId;
    private final AtomicInteger interactionState = new AtomicInteger(0);

    public PlacedFurniture(UUID uuid, String configId, @NotNull Location location,
                           UUID ownerUuid, int virtualEntityId) {
        this.uuid = uuid;
        this.configId = configId;
        this.location = location.clone();
        this.ownerUuid = ownerUuid;
        this.virtualEntityId = virtualEntityId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getConfigId() {
        return configId;
    }

    public @NotNull Location getLocation() {
        return location.clone();
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public int getVirtualEntityId() {
        return virtualEntityId;
    }

    public int getInteractionState() {
        return interactionState.get();
    }

    public void setInteractionState(int v) {
        interactionState.set(v);
    }
}
