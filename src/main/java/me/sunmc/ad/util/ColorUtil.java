package me.sunmc.ad.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class ColorUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ColorUtil() {
    }

    /**
     * Deserialise a MiniMessage string into a Component.
     */
    @Contract("_ -> new")
    public static @NotNull Component component(String s) {
        return MM.deserialize(s == null ? "" : s);
    }
}