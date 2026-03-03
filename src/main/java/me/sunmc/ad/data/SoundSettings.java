package me.sunmc.ad.data;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

/**
 * Encapsulates a configurable in-world sound effect.
 * Sound names in config.yml must be namespaced keys, e.g. "minecraft:block.wood.place".
 */
public final class SoundSettings {

    private final boolean enabled;
    private final String sound;
    private final float volume;
    private final float pitch;

    public SoundSettings(boolean enabled, String sound, float volume, float pitch) {
        this.enabled = enabled;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    /**
     * Resolves a namespaced key string to a {@link Sound} via {@link Registry#SOUNDS}.
     * Returns null for unknown or malformed keys — no exception thrown.
     */
    private static Sound resolve(@NotNull String name) {
        NamespacedKey key = NamespacedKey.fromString(name.trim().toLowerCase());
        if (key == null) return null;
        return Registry.SOUNDS.get(key);
    }

    public void play(Location loc) {
        if (!enabled || loc == null || loc.getWorld() == null) return;
        if (sound == null || sound.isBlank()) return;

        Sound resolved = resolve(sound);
        if (resolved == null) return;
        loc.getWorld().playSound(loc, resolved, volume, pitch);
    }
}