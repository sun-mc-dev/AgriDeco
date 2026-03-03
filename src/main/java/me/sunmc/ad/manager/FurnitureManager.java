package me.sunmc.ad.manager;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.data.FurnitureDef;
import me.sunmc.ad.data.PlacedFurniture;
import me.sunmc.ad.data.enums.FurnitureType;
import me.sunmc.ad.gui.FurnitureContainerHolder;
import me.sunmc.ad.packet.VirtualEntity;
import me.sunmc.ad.util.ChunkKey;
import me.sunmc.ad.util.ColorUtil;
import me.sunmc.ad.util.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FurnitureManager {

    private final AgriDeco plugin;

    private final ConcurrentHashMap<UUID, PlacedFurniture> byUuid = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, UUID> byEntityId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> chunkCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, UUID> barrierLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> seatStands = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<UUID>> byChunk = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> placeCooldowns = new ConcurrentHashMap<>();

    public FurnitureManager(AgriDeco plugin) {
        this.plugin = plugin;
    }


    public void loadFromDatabase() {
        plugin.getDataRepository().loadAllFurniture().thenAccept(list -> {
            for (var pf : list) {
                register(pf);
                plugin.getFoliaScheduler().runAt(pf.getLocation(), () -> {
                    var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
                    if (def != null) trackBarriers(def, pf.getLocation(), pf.getUuid());
                    spawnVisual(pf);
                }, () -> plugin.getSLF4JLogger().warn(
                        "Chunk unloaded before furniture visual could spawn: {}", pf.getUuid()));
            }
            plugin.getSLF4JLogger().info("Loaded {} furniture pieces.", list.size());
        });
    }

    public boolean place(Player player, String configId, Location loc) {
        var def = plugin.getConfigManager().getFurnitureDefs().get(configId);
        if (def == null) return false;

        // Per-item place permission
        if (!def.getPlacePermission().isEmpty() && !player.hasPermission(def.getPlacePermission())) {
            plugin.getMessagesManager().send(player, "no_permission");
            return false;
        }
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
        if (!checkCooldown(player)) return false;

        var pf = new PlacedFurniture(UUID.randomUUID(), configId, loc, player.getUniqueId(), VirtualEntity.nextId());
        register(pf);
        placeBarriers(def, loc, pf.getUuid());
        spawnVisual(pf);
        plugin.getDataRepository().saveFurniture(pf);
        plugin.getConfigManager().getSoundPlaceFurniture().play(loc);
        plugin.getMessagesManager().send(player, "placed_furniture");
        return true;
    }

    public boolean remove(Player player, UUID furnitureUuid) {
        var pf = byUuid.get(furnitureUuid);
        if (pf == null) return false;
        var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
        var loc = pf.getLocation();

        unregister(pf);
        if (def != null) removeBarriers(def, loc);
        plugin.getPacketHandler().despawnEntity(pf.getVirtualEntityId(), loc);
        plugin.getDataRepository().deleteFurniture(pf.getUuid());

        // Drop item if globally and per-item configured
        if (def != null && plugin.getConfigManager().isRemovalDropItem() && def.isRemoveDropItem()) {
            var base = plugin.getIntegrations().getMmoItem(def.getId());
            if (base != null)
                loc.getWorld().dropItemNaturally(loc, ItemUtil.tagFurniture(base, pf.getConfigId()));
        }

        plugin.getConfigManager().getSoundRemoveFurniture().play(loc);
        plugin.getMessagesManager().send(player, "removed_furniture");
        return true;
    }

    private void spawnVisual(@NotNull PlacedFurniture pf) {
        var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
        if (def == null) return;
        var head = resolveHead(def, pf.getInteractionState());
        var visual = pf.getLocation().add(def.getArmorstandOffset());
        plugin.getPacketHandler().spawnArmorStand(
                pf.getVirtualEntityId(), visual, head,
                def.isArmorstandBaby(), true,
                p -> handleClick(p, pf),
                p -> handleAttack(p, pf));  // attack = left-click = removal
    }

    private void handleAttack(Player player, @NotNull PlacedFurniture pf) {
        var cfg = plugin.getConfigManager();
        if (!cfg.isRemovalEnabled()) return;
        if (cfg.isRemovalSneakRequired() && !player.isSneaking()) {
            plugin.getMessagesManager().send(player, "sneak_to_remove");
            return;
        }
        var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
        // Per-item remove permission
        if (def != null && !def.getRemovePermission().isEmpty()
                && !player.hasPermission(def.getRemovePermission())) {
            plugin.getMessagesManager().send(player, "no_permission");
            return;
        }
        // Ownership check (admins bypass via reload permission)
        if (cfg.isRemovalOwnerOnly()
                && !pf.getOwnerUuid().equals(player.getUniqueId())
                && !player.hasPermission(cfg.getReloadPermission())) {
            plugin.getMessagesManager().send(player, "not_owner");
            return;
        }
        plugin.getFoliaScheduler().runAt(pf.getLocation(), () -> remove(player, pf.getUuid()));
    }

    private void handleClick(Player player, @NotNull PlacedFurniture pf) {
        var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
        if (def == null) return;
        switch (def.getFurnitureType()) {
            case INTERACTABLE -> handleInteract(pf);
            case CONTAINER -> openContainer(player, def);
            case SEAT -> seatPlayer(player, pf, def);
            default -> {
            }
        }
    }

    private void handleInteract(@NotNull PlacedFurniture pf) {
        pf.setInteractionState((pf.getInteractionState() + 1) % 2);
        plugin.getPacketHandler().despawnEntity(pf.getVirtualEntityId(), pf.getLocation());
        spawnVisual(pf);
        plugin.getDataRepository().saveFurniture(pf);
    }

    private void openContainer(@NotNull Player player, @NotNull FurnitureDef def) {
        var holder = new FurnitureContainerHolder();
        var inv = plugin.getServer().createInventory(
                holder, def.getContainerSize(), ColorUtil.component(def.getContainerTitle()));
        holder.setInventory(inv);
        player.openInventory(inv);
    }

    private void seatPlayer(Player player, @NotNull PlacedFurniture pf, @NotNull FurnitureDef def) {
        var seatLoc = pf.getLocation().add(def.getSeatOffset());
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

    private boolean checkCooldown(Player player) {
        int secs = plugin.getConfigManager().getPlacementCooldownSeconds();
        if (secs <= 0) return true;
        long now = System.currentTimeMillis();
        long last = placeCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long elapsed = now - last;
        if (elapsed < secs * 1000L) {
            long remaining = (secs * 1000L - elapsed + 999) / 1000;
            plugin.getMessagesManager().send(player, "placement_cooldown",
                    "{seconds}", String.valueOf(remaining));
            return false;
        }
        placeCooldowns.put(player.getUniqueId(), now);
        return true;
    }

    public void spawnVisualForPlayer(@NotNull PlacedFurniture pf, Player player) {
        var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
        if (def == null) return;
        var head = resolveHead(def, pf.getInteractionState());
        var visual = pf.getLocation().add(def.getArmorstandOffset());
        plugin.getPacketHandler().spawnArmorStandForPlayer(
                player, pf.getVirtualEntityId(), visual, head, def.isArmorstandBaby(), true);
    }

    private @NotNull ItemStack resolveHead(@NotNull FurnitureDef def, int state) {
        String mmoId = (def.getFurnitureType() == FurnitureType.INTERACTABLE && state == 1)
                ? def.getInteractionId() : def.getId();
        var item = plugin.getIntegrations().getMmoItem(mmoId);
        return item != null ? item : ItemStack.of(Material.BARRIER);
    }

    private void placeBarriers(@NotNull FurnitureDef def, Location loc, UUID uuid) {
        for (var offset : def.getBarrierOffsets()) {
            var bl = loc.clone().add(offset);
            Block b = bl.getBlock();
            if (b.getType() == Material.AIR) b.setType(Material.BARRIER);
            barrierLocations.put(ChunkKey.block(bl), uuid);
        }
    }

    private void trackBarriers(@NotNull FurnitureDef def, Location loc, UUID uuid) {
        for (var offset : def.getBarrierOffsets())
            barrierLocations.put(ChunkKey.block(loc.clone().add(offset)), uuid);
    }

    private void removeBarriers(@Nullable FurnitureDef def, Location loc) {
        if (def == null) return;
        for (var offset : def.getBarrierOffsets()) {
            var bl = loc.clone().add(offset);
            barrierLocations.remove(ChunkKey.block(bl));
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

    private void register(@NotNull PlacedFurniture pf) {
        byUuid.put(pf.getUuid(), pf);
        byEntityId.put(pf.getVirtualEntityId(), pf.getUuid());
        chunkCount.merge(ChunkKey.of(pf.getLocation()), 1, Integer::sum);
        byChunk.computeIfAbsent(ChunkKey.of(pf.getLocation()), k -> new CopyOnWriteArrayList<>())
                .add(pf.getUuid());
    }

    private void unregister(@NotNull PlacedFurniture pf) {
        byUuid.remove(pf.getUuid());
        byEntityId.remove(pf.getVirtualEntityId());
        long ck = ChunkKey.of(pf.getLocation());
        chunkCount.computeIfPresent(ck, (k, v) -> v <= 1 ? null : v - 1);
        var bucket = byChunk.get(ck);
        if (bucket != null) {
            bucket.remove(pf.getUuid());
            if (bucket.isEmpty()) byChunk.remove(ck);
        }
    }

    public void sendVisibleFurnitureTo(@NotNull Player player) {
        int maxDist = plugin.getConfigManager().getMaxArmorStandDistance();
        int chunkRadius = (maxDist >> 4) + 1;
        int pcx = player.getLocation().getBlockX() >> 4;
        int pcz = player.getLocation().getBlockZ() >> 4;
        long maxDistSq = (long) maxDist * maxDist;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                var bucket = byChunk.get(ChunkKey.of(pcx + dx, pcz + dz));
                if (bucket == null) continue;
                for (var uuid : bucket) {
                    var pf = byUuid.get(uuid);
                    if (pf == null || !pf.getWorldName().equals(player.getWorld().getName())) continue;
                    double ddx = pf.getX() - player.getX(), ddy = pf.getY() - player.getY(), ddz = pf.getZ() - player.getZ();
                    if (ddx * ddx + ddy * ddy + ddz * ddz > maxDistSq) continue;
                    plugin.getFoliaScheduler().runAt(pf.getLocation(), () -> spawnVisualForPlayer(pf, player));
                }
            }
        }
    }

    public boolean isFurnitureBarrier(Location loc) {
        return barrierLocations.containsKey(ChunkKey.block(loc));
    }

    public boolean isSeatStand(UUID entityUuid) {
        return seatStands.containsKey(entityUuid);
    }

    public void removeSeatStand(UUID standUuid) {
        seatStands.remove(standUuid);
    }

    public Optional<UUID> getFurnitureByEntityId(int vid) {
        return Optional.ofNullable(byEntityId.get(vid));
    }

    public void reload() {
        placeCooldowns.clear();
        plugin.getSLF4JLogger().warn("FurnitureManager: reload applied. Restart required for structural changes.");
    }

    public void shutdown() {
    }

    /**
     * Returns the PlacedFurniture for a UUID, or null if not found.
     */
    public @Nullable PlacedFurniture getByUuid(UUID uuid) {
        return byUuid.get(uuid);
    }

    /**
     * Admin-only hard removal — bypasses all ownership / permission / sneak checks.
     * Drops NO item regardless of config. Intended for /agrideco remove and cleanup scripts.
     */
    public boolean adminRemove(UUID furnitureUuid) {
        var pf = byUuid.get(furnitureUuid);
        if (pf == null) return false;
        var def = plugin.getConfigManager().getFurnitureDefs().get(pf.getConfigId());
        var loc = pf.getLocation();
        unregister(pf);
        if (def != null) removeBarriers(def, loc);
        plugin.getPacketHandler().despawnEntity(pf.getVirtualEntityId(), loc);
        plugin.getDataRepository().deleteFurniture(pf.getUuid());
        plugin.getSLF4JLogger().info("Admin removed furniture {} ({}) at {}",
                pf.getUuid(), pf.getConfigId(), loc.toVector());
        return true;
    }
}