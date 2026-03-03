package me.sunmc.ad.util;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class DirectionUtil {

    private DirectionUtil() {
    }

    @Contract("null -> new")
    public static @NotNull Vector parseVector(String s) {
        if (s == null || s.isBlank()) return new Vector();
        try {
            var parts = s.replaceAll("\\s", "").split(",");
            return new Vector(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
        } catch (Exception e) {
            return new Vector();
        }
    }
}
