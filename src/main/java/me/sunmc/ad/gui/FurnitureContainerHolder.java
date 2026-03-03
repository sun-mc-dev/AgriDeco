package me.sunmc.ad.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Fix #8: a real InventoryHolder for furniture containers so the
 * InventoryClickListener instanceof BaseGui check can be extended
 * and clicks on this inventory are properly cancelled.
 * <p>
 * InventoryClickListener already cancels any top inventory whose
 * holder is a BaseGui. For furniture containers we just need any
 * non-null holder so Bukkit doesn't orphan click events — the
 * InventoryClickListener is updated to also cancel this holder.
 */
public final class FurnitureContainerHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}