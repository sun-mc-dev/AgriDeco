package me.sunmc.ad.listener;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.gui.BaseGui;
import me.sunmc.ad.gui.FurnitureContainerHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

public final class InventoryClickListener implements Listener {

    private final AgriDeco plugin;

    public InventoryClickListener(AgriDeco plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(@NotNull InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        var holder = e.getView().getTopInventory().getHolder();

        if (holder instanceof BaseGui gui) {
            e.setCancelled(true);
            if (e.getClickedInventory() != null &&
                    e.getClickedInventory().equals(e.getView().getTopInventory()))
                gui.onClick(e);
            return;
        }

        if (holder instanceof FurnitureContainerHolder) {
            // Allow normal use (put/take items) but block shift-click out to player inventory
            if (e.isShiftClick()) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(@NotNull InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof BaseGui)) return;
        plugin.getGuiManager().remove(player);
    }
}