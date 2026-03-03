package me.sunmc.ad.manager;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.data.FurnitureDef;
import me.sunmc.ad.data.PlacedFurniture;
import me.sunmc.ad.data.enums.FurnitureType;
import me.sunmc.ad.packet.VirtualEntity;
import me.sunmc.ad.util.ChunkKey;
import me.sunmc.ad.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FurnitureManager {

    private final AgriDeco plugin;

    private final ConcurrentHashMap<UUID, PlacedFurniture> byUuid = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, UUID> byEntityId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> chunkCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, UUID> barrierLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> seatStands = new ConcurrentHashMap<>();

    public FurnitureManager(AgriDeco plugin) {
        this.plugin = plugin;
    }

    private static long blockKey(@NotNull Location loc) {
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    public void loadFromDatabase() {
        plugin.getDataRepository().loadAllFurniture().thenAccept(list -> {
            for (var pf : list) {
                register(pf);
                plugin.getFoliaScheduler().runAt(pf.getLocation(), () -> {
                    var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
                    if (def != null) trackBarriers(def, pf.getLocation(), pf.getUuid());
                    spawnVisual(pf);
                });
            }
            plugin.getSLF4JLogger().info("Loaded {} furniture pieces.", list.size());
        });
    }

    public boolean place(Player player, String configId, Location loc) {
        var def = plugin.getConfigManager().getFurnitureDefs().get(configId);
        if (def == null) return false;

        if (!isValidPlacement(def, loc)) {
            plugin.getMessagesManager().send(player, "invalid_placement");
            return false;
        }

        long ck = ChunkKey.of(loc);
        if (chunkCount.getOrDefault(ck, 0) >= plugin.getConfigManager().getChunkLimitFurnitures()) {
            plugin.getMessagesManager().send(player, "chunk_limit");
            return false;
        }

        if (!plugin.getIntegrations().canBuild(player, loc)) {
            plugin.getMessagesManager().send(player, "invalid_placement");
            return false;
        }

        var pf = new PlacedFurniture(UUID.randomUUID(), configId, loc,
                player.getUniqueId(), VirtualEntity.nextId());
        register(pf);
        placeBarriers(def, loc, pf.getUuid());
        spawnVisual(pf);
        plugin.getDataRepository().saveFurniture(pf);
        plugin.getMessagesManager().send(player, "placed_furniture");
        return true;
    }

    public boolean remove(Player player, UUID furnitureUuid) {
        var pf = byUuid.get(furnitureUuid);
        if (pf == null) return false;
        var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
        unregister(pf);
        removeBarriers(def, pf.getLocation());
        plugin.getPacketHandler().despawnEntity(pf.getVirtualEntityId(), pf.getLocation());
        plugin.getDataRepository().deleteFurniture(pf.getUuid());
        plugin.getMessagesManager().send(player, "removed_furniture");
        return true;
    }

    private void spawnVisual(@NotNull PlacedFurniture pf) {
        var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
        if (def == null) return;
        var head = resolveHead(def, pf.getInteractionState());
        var visual = pf.getLocation().clone().add(def.getArmorstandOffset());
        plugin.getPacketHandler().spawnArmorStand(
                pf.getVirtualEntityId(), visual, head,
                def.isArmorstandBaby(), true,
                p -> handleClick(p, pf));
    }

    public void spawnVisualForPlayer(@NotNull PlacedFurniture pf, Player player) {
        var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
        if (def == null) return;
        var head = resolveHead(def, pf.getInteractionState());
        var visual = pf.getLocation().clone().add(def.getArmorstandOffset());
        plugin.getPacketHandler().spawnArmorStandForPlayer(
                player, pf.getVirtualEntityId(), visual, head, def.isArmorstandBaby(), true);
    }

    private @NotNull ItemStack resolveHead(@NotNull FurnitureDef def, int state) {
        String mmoId = (def.getFurnitureType() == FurnitureType.INTERACTABLE && state == 1)
                ? def.getInteractionId() : def.getId();
        var item = plugin.getIntegrations().getMmoItem(mmoId);
        return item != null ? item : new ItemStack(Material.BARRIER);
    }

    private void handleClick(Player player, @NotNull PlacedFurniture pf) {
        var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
        if (def == null) return;
        switch (def.getFurnitureType()) {
            case INTERACTABLE -> handleInteract(pf, def);
            case CONTAINER -> openContainer(player, def);
            case SEAT -> seatPlayer(player, pf, def);
            default -> {
            }
        }
    }

    private void handleInteract(@NotNull PlacedFurniture pf, FurnitureDef def) {
        pf.setInteractionState((pf.getInteractionState() + 1) % 2);
        plugin.getPacketHandler().despawnEntity(pf.getVirtualEntityId(), pf.getLocation());
        spawnVisual(pf);
        plugin.getDataRepository().saveFurniture(pf);
    }

    private void openContainer(@NotNull Player player, @NotNull FurnitureDef def) {
        var inv = plugin.getServer().createInventory(
                null, def.getContainerSize(),
                ColorUtil.component(def.getContainerTitle()));
        player.openInventory(inv);
    }

    private void seatPlayer(Player player, @NotNull PlacedFurniture pf, @NotNull FurnitureDef def) {
        var seatLoc = pf.getLocation().clone().add(def.getSeatOffset());
        plugin.getFoliaScheduler().runAt(seatLoc, () -> {
            var stand = seatLoc.getWorld().spawn(seatLoc, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setMarker(false);
                as.setSmall(true);
                as.setInvulnerable(true);
                as.setCollidable(false);
            });
            seatStands.put(stand.getUniqueId(), pf.getUuid());
            stand.addPassenger(player);
        });
    }

    public boolean isSeatStand(UUID entityUuid) {
        return seatStands.containsKey(entityUuid);
    }

    public void removeSeatStand(UUID standUuid) {
        seatStands.remove(standUuid);
    }

    public boolean isFurnitureBarrier(Location loc) {
        return barrierLocations.containsKey(blockKey(loc));
    }

    private void placeBarriers(@NotNull FurnitureDef def, Location loc, UUID furnitureUuid) {
        for (var offset : def.getBarrierOffsets()) {
            var bl = loc.clone().add(offset);
            Block b = bl.getBlock();
            if (b.getType() == Material.AIR) b.setType(Material.BARRIER);
            barrierLocations.put(blockKey(bl), furnitureUuid);
        }
    }

    private void trackBarriers(@NotNull FurnitureDef def, Location loc, UUID furnitureUuid) {
        for (var offset : def.getBarrierOffsets())
            barrierLocations.put(blockKey(loc.clone().add(offset)), furnitureUuid);
    }

    private void removeBarriers(FurnitureDef def, Location loc) {
        if (def == null) return;
        for (var offset : def.getBarrierOffsets()) {
            var bl = loc.clone().add(offset);
            barrierLocations.remove(blockKey(bl));
            Block b = bl.getBlock();
            if (b.getType() == Material.BARRIER) b.setType(Material.AIR);
        }
    }

    private boolean isValidPlacement(@NotNull FurnitureDef def, Location loc) {
        return switch (def.getPlacementType()) {
            case FLOOR -> loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid();
            case CEILING -> loc.clone().add(0, 1, 0).getBlock().getType().isSolid();
            case WALL -> loc.getBlock().getType() == Material.AIR;
        };
    }

    private void register(PlacedFurniture pf) {
        byUuid.put(pf.getUuid(), pf);
        byEntityId.put(pf.getVirtualEntityId(), pf.getUuid());
        chunkCount.merge(ChunkKey.of(pf.getLocation()), 1, Integer::sum);
    }

    private void unregister(@NotNull PlacedFurniture pf) {
        byUuid.remove(pf.getUuid());
        byEntityId.remove(pf.getVirtualEntityId());
        chunkCount.computeIfPresent(ChunkKey.of(pf.getLocation()),
                (k, v) -> v <= 1 ? null : v - 1);
    }

    public void sendVisibleFurnitureTo(Player player) {
        int maxDist = plugin.getConfigManager().getMaxArmorStandDistance();
        long maxDistSq = (long) maxDist * maxDist;
        for (var pf : byUuid.values()) {
            if (!pf.getLocation().getWorld().equals(player.getWorld())) continue;
            if (pf.getLocation().distanceSquared(player.getLocation()) > maxDistSq) continue;
            plugin.getFoliaScheduler().runAt(pf.getLocation(),
                    () -> spawnVisualForPlayer(pf, player));
        }
    }

    public Optional<UUID> getFurnitureByEntityId(int vid) {
        return Optional.ofNullable(byEntityId.get(vid));
    }

    public void reload() {
    }

    public void shutdown() {
    }
}