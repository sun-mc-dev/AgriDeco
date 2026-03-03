package me.sunmc.ad.listener;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.util.ItemUtil;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

public final class PlayerInteractListener implements Listener {

    private final AgriDeco plugin;

    public PlayerInteractListener(AgriDeco plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (e.getBlockFace() == BlockFace.DOWN) return;

        var player = e.getPlayer();
        var item = player.getInventory().getItemInMainHand();

        var furnitureId = ItemUtil.getFurnitureId(item);
        if (furnitureId != null) {
            var fm = plugin.getFurnitureManager();
            if (fm == null) return;
            var placeLoc = e.getClickedBlock().getRelative(e.getBlockFace()).getLocation().add(0.5, 0, 0.5);
            placeLoc.setYaw(player.getLocation().getYaw());
            plugin.getFoliaScheduler().runAt(placeLoc, () -> fm.place(player, furnitureId, placeLoc));
            e.setCancelled(true);
            return;
        }

        var cropId = ItemUtil.getCropId(item);
        if (cropId != null) {
            var cm = plugin.getCropManager();
            if (cm == null) return;
            var plantLoc = e.getClickedBlock().getRelative(e.getBlockFace()).getLocation().add(0.5, 0, 0.5);
            plugin.getFoliaScheduler().runAt(plantLoc, () -> cm.plant(player, cropId, plantLoc));
            e.setCancelled(true);
        }
    }
}
