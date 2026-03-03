package me.sunmc.ad.util;

import me.sunmc.ad.Keys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemUtil {

    private ItemUtil() {
    }

    public static @NotNull ItemStack tagFurniture(@NotNull ItemStack base, String configId) {
        var item = base.clone();
        editMeta(item, meta ->
                meta.getPersistentDataContainer()
                        .set(Keys.FURNITURE_ID, PersistentDataType.STRING, configId));
        return item;
    }

    public static @NotNull ItemStack tagCrop(@NotNull ItemStack base, String configId) {
        var item = base.clone();
        editMeta(item, meta ->
                meta.getPersistentDataContainer()
                        .set(Keys.CROP_ID, PersistentDataType.STRING, configId));
        return item;
    }

    @Nullable
    public static String getFurnitureId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(Keys.FURNITURE_ID, PersistentDataType.STRING);
    }

    @Nullable
    public static String getCropId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(Keys.CROP_ID, PersistentDataType.STRING);
    }

    private static void editMeta(@NotNull ItemStack item,
                                 java.util.function.Consumer<org.bukkit.inventory.meta.ItemMeta> edit) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        edit.accept(meta);
        item.setItemMeta(meta);
    }
}