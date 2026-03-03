package me.sunmc.ad.data.enums;

import org.jetbrains.annotations.NotNull;

public enum FurnitureType {
    DECORATIVE, INTERACTABLE, CONTAINER, SEAT, LIGHT;

    public static FurnitureType fromString(@NotNull String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DECORATIVE;
        }
    }
}