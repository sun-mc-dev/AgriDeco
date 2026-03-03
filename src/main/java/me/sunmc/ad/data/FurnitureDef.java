package me.sunmc.ad.data;

import me.sunmc.ad.data.enums.FurnitureType;
import me.sunmc.ad.data.enums.PlacementType;
import me.sunmc.ad.util.DirectionUtil;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FurnitureDef {

    private final String id;
    private final PlacementType placementType;
    private final FurnitureType furnitureType;
    private final String interactionId;
    private final String containerTitle;
    private final int containerSize;
    private final Vector armorstandOffset;
    private final boolean armorstandBaby;
    private final Vector seatOffset;
    private final List<Vector> barrierOffsets;

    private FurnitureDef(@NotNull Builder b) {
        id = b.id;
        placementType = b.placementType;
        furnitureType = b.furnitureType;
        interactionId = b.interactionId;
        containerTitle = b.containerTitle;
        containerSize = b.containerSize;
        armorstandOffset = DirectionUtil.parseVector(b.armorstandOffset);
        armorstandBaby = b.armorstandBaby;
        seatOffset = DirectionUtil.parseVector(b.seatOffset);
        barrierOffsets = b.barrierOffsets.stream().map(DirectionUtil::parseVector).toList();
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public PlacementType getPlacementType() {
        return placementType;
    }

    public FurnitureType getFurnitureType() {
        return furnitureType;
    }

    public String getInteractionId() {
        return interactionId;
    }

    public String getContainerTitle() {
        return containerTitle;
    }

    public int getContainerSize() {
        return containerSize;
    }

    public @NotNull Vector getArmorstandOffset() {
        return armorstandOffset.clone();
    }

    public boolean isArmorstandBaby() {
        return armorstandBaby;
    }

    public @NotNull Vector getSeatOffset() {
        return seatOffset.clone();
    }

    public List<Vector> getBarrierOffsets() {
        return barrierOffsets;
    }

    public static final class Builder {
        private String id = "";
        private PlacementType placementType = PlacementType.FLOOR;
        private FurnitureType furnitureType = FurnitureType.DECORATIVE;
        private String interactionId = "";
        private String containerTitle = "&8Container";
        private int containerSize = 9;
        private String armorstandOffset = "0,0,0";
        private boolean armorstandBaby = false;
        private String seatOffset = "0.5,0,0.5";
        private List<String> barrierOffsets = List.of("0,0,0");

        public Builder id(String v) {
            id = v;
            return this;
        }

        public Builder placementType(PlacementType v) {
            placementType = v;
            return this;
        }

        public Builder furnitureType(FurnitureType v) {
            furnitureType = v;
            return this;
        }

        public Builder interactionId(String v) {
            interactionId = v;
            return this;
        }

        public Builder containerTitle(String v) {
            containerTitle = v;
            return this;
        }

        public Builder containerSize(int v) {
            containerSize = v;
            return this;
        }

        public Builder armorstandOffset(String v) {
            armorstandOffset = v;
            return this;
        }

        public Builder armorstandBaby(boolean v) {
            armorstandBaby = v;
            return this;
        }

        public Builder seatOffset(String v) {
            seatOffset = v;
            return this;
        }

        public Builder barrierOffsets(List<String> v) {
            barrierOffsets = v;
            return this;
        }

        @Contract(" -> new")
        public @NotNull FurnitureDef build() {
            return new FurnitureDef(this);
        }
    }
}