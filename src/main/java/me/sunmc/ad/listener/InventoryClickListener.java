package me.sunmc.ad.listener;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.gui.BaseGui;
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
        if (!(e.getView().getTopInventory().getHolder() instanceof BaseGui gui)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != null &&
                e.getClickedInventory().equals(e.getView().getTopInventory()))
            gui.onClick(e);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(@NotNull InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof BaseGui)) return;
        plugin.getGuiManager().remove(player);
    }
}