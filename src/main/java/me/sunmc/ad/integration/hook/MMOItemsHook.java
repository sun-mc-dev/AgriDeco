package me.sunmc.ad.integration.hook;

import me.sunmc.ad.AgriDeco;
import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class MMOItemsHook {

    private final boolean enabled;

    public MMOItemsHook(AgriDeco plugin, boolean enabled) {
        this.enabled = enabled;
        if (enabled) plugin.getSLF4JLogger().info("MMOItems hooked.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    public ItemStack getItem(String mmoId) {
        if (!enabled || mmoId == null || mmoId.isBlank()) return null;
        try {
            var parts = mmoId.split(":");
            if (parts.length < 3) return null;
            var type = MMOItems.plugin.getTypes().get(parts[1]);
            if (type == null) return null;
            var template = MMOItems.plugin.getTemplates().getTemplate(type, parts[2]);
            if (template == null) return null;
            return template.newBuilder().build().getType().getItem();
        } catch (Exception e) {
            return null;
        }
    }
}