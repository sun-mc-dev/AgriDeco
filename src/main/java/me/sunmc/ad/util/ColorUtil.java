package me.sunmc.ad.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class ColorUtil {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private ColorUtil() {
    }

    @Contract(pure = true)
    public static @NotNull String translate(String s) {
        return s == null ? "" : s.replace("&", "§");
    }

    public static @NotNull Component component(String s) {
        return LEGACY.deserialize(s == null ? "" : s);
    }
}