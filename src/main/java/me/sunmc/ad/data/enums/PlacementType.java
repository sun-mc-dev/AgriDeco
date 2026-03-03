package me.sunmc.ad.data.enums;

import org.jetbrains.annotations.NotNull;

public enum PlacementType {
    FLOOR, WALL, CEILING;

    public static PlacementType fromString(@NotNull String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FLOOR;
        }
    }
}