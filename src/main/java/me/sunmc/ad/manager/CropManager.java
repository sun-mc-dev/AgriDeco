package me.sunmc.ad.manager;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.data.CropDef;
import me.sunmc.ad.data.PlacedCrop;
import me.sunmc.ad.packet.VirtualEntity;
import me.sunmc.ad.util.ChunkKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class CropManager {

    private final AgriDeco plugin;

    private final ConcurrentHashMap<UUID, PlacedCrop> byUuid = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, UUID> byEntityId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> chunkCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ScheduledTask> growthTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<UUID>> byChunk = new ConcurrentHashMap<>();

    public CropManager(AgriDeco plugin) {
        this.plugin = plugin;
    }

    public void loadFromDatabase() {
        plugin.getDataRepository().loadAllCrops().thenAccept(list -> {
            for (var crop : list) {
                register(crop);

                plugin.getFoliaScheduler().runAt(crop.getLocation(), () -> {
                    spawnVisual(crop);
                    scheduleGrowth(crop);
                }, () -> plugin.getSLF4JLogger().warn(
                        "Chunk unloaded before crop visual could spawn: {}", crop.getUuid()));
            }
            plugin.getSLF4JLogger().info("Loaded {} crops.", list.size());
        });
    }

    public boolean plant(Player player, String configId, Location loc) {
        var def = plugin.getConfigManager().getCropDefs().get(configId);
        if (def == null) return false;
        if (!loc.clone().subtract(0, 1, 0).getBlock().getType().name()
                .equalsIgnoreCase(def.getPlacementBlock())) {
            plugin.getMessagesManager().send(player, "invalid_placement");
            return false;
        }
        long ck = ChunkKey.of(loc);
        if (chunkCount.getOrDefault(ck, 0) >= plugin.getConfigManager().getChunkLimitCrops()) {
            plugin.getMessagesManager().send(player, "chunk_limit");
            return false;
        }
        if (!plugin.getIntegrations().canBuild(player, loc)) {
            plugin.getMessagesManager().send(player, "invalid_placement");
            return false;
        }
        var crop = new PlacedCrop(UUID.randomUUID(), configId, loc, player.getUniqueId(), VirtualEntity.nextId());
        register(crop);
        spawnVisual(crop);
        scheduleGrowth(crop);
        plugin.getDataRepository().saveCrop(crop);
        plugin.getMessagesManager().send(player, "placed_crop");
        return true;
    }

    public boolean harvest(Player player, UUID cropUuid) {
        var crop = byUuid.get(cropUuid);
        if (crop == null) return false;
        var def = plugin.getConfigManager().getCropDefs().get(crop.getConfigId());
        if (def == null) return false;
        if (!def.isFullyGrown(crop.getStage())) {
            plugin.getMessagesManager().send(player, "crop_not_ready");
            return false;
        }
        plugin.getFoliaScheduler().runAsync(() -> {
            plugin.getIntegrations().grantAuraExp(player, def.getAuraExp());
            plugin.getIntegrations().grantJobReward(player, def.getJobId(), def.getJobExp(), def.getJobMoney());
        });
        var drop = plugin.getIntegrations().getMmoItem(def.getStageIds().get(def.getStageIds().size() - 1));
        if (drop != null)
            crop.getLocation().getWorld().dropItemNaturally(crop.getLocation(), drop);

        unregister(crop);
        plugin.getPacketHandler().despawnEntity(crop.getVirtualEntityId(), crop.getLocation());
        plugin.getDataRepository().deleteCrop(crop.getUuid());
        plugin.getMessagesManager().send(player, "harvested_crop");
        return true;
    }

    private void scheduleGrowth(@NotNull PlacedCrop crop) {
        var def = plugin.getConfigManager().getCropDefs().get(crop.getConfigId());
        if (def == null || def.isFullyGrown(crop.getStage())) return;
        var task = plugin.getFoliaScheduler().runAtFixedRate(
                crop.getLocation(), () -> tickGrowth(crop),
                def.getGrowthTimeTicks(), def.getGrowthTimeTicks());
        growthTasks.put(crop.getUuid(), task);
    }

    private void tickGrowth(@NotNull PlacedCrop crop) {
        if (!byUuid.containsKey(crop.getUuid())) {
            cancelGrowth(crop.getUuid());
            return;
        }
        var def = plugin.getConfigManager().getCropDefs().get(crop.getConfigId());
        if (def == null) return;
        if (ThreadLocalRandom.current().nextInt(100) >= def.getGrowthChance()) return;
        int cur = crop.getStage();
        if (def.isFullyGrown(cur)) {
            cancelGrowth(crop.getUuid());
            return;
        }
        if (!crop.tryAdvanceStage(cur, cur + 1)) return;

        UUID cropUuid = crop.getUuid();
        plugin.getPacketHandler().despawnEntity(crop.getVirtualEntityId(), crop.getLocation());
        spawnVisual(crop);
        plugin.getDataRepository().saveCrop(crop);
        if (def.isFullyGrown(crop.getStage())) cancelGrowth(cropUuid);
    }

    private void cancelGrowth(UUID uuid) {
        var task = growthTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private void spawnVisual(@NotNull PlacedCrop crop) {
        var def = plugin.getConfigManager().getCropDefs().get(crop.getConfigId());
        if (def == null) return;
        var head = resolveStageHead(def, crop.getStage());
        var visual = crop.getLocation().add(def.getArmorstandOffset());

        UUID cropUuid = crop.getUuid();
        plugin.getPacketHandler().spawnArmorStand(
                crop.getVirtualEntityId(), visual, head,
                def.isArmorstandBaby(), true,
                p -> harvest(p, cropUuid));
    }

    public void spawnVisualForPlayer(@NotNull PlacedCrop crop, Player player) {
        var def = plugin.getConfigManager().getCropDefs().get(crop.getConfigId());
        if (def == null) return;
        var head = resolveStageHead(def, crop.getStage());
        var visual = crop.getLocation().add(def.getArmorstandOffset());
        plugin.getPacketHandler().spawnArmorStandForPlayer(
                player, crop.getVirtualEntityId(), visual, head, def.isArmorstandBaby(), true);
    }

    private @NotNull ItemStack resolveStageHead(@NotNull CropDef def, int stage) {
        var stageId = def.getStageIds().get(Math.min(stage, def.getStageIds().size() - 1));
        var item = plugin.getIntegrations().getMmoItem(stageId);
        return item != null ? item : ItemStack.of(Material.WHEAT_SEEDS);
    }

    private void register(@NotNull PlacedCrop crop) {
        byUuid.put(crop.getUuid(), crop);
        byEntityId.put(crop.getVirtualEntityId(), crop.getUuid());
        long ck = ChunkKey.of(crop.getLocation());
        chunkCount.merge(ck, 1, Integer::sum);
        byChunk.computeIfAbsent(ck, k -> new ArrayList<>()).add(crop.getUuid());
    }

    private void unregister(@NotNull PlacedCrop crop) {
        cancelGrowth(crop.getUuid());
        byUuid.remove(crop.getUuid());
        byEntityId.remove(crop.getVirtualEntityId());
        long ck = ChunkKey.of(crop.getLocation());
        chunkCount.computeIfPresent(ck, (k, v) -> v <= 1 ? null : v - 1);
        var bucket = byChunk.get(ck);
        if (bucket != null) {
            bucket.remove(crop.getUuid());
            if (bucket.isEmpty()) byChunk.remove(ck);
        }
    }

    public void sendVisibleCropsTo(@NotNull Player player) {
        int maxDist = plugin.getConfigManager().getMaxArmorStandDistance();
        int chunkRadius = (maxDist >> 4) + 1;
        int pcx = player.getLocation().getBlockX() >> 4;
        int pcz = player.getLocation().getBlockZ() >> 4;
        long maxDistSq = (long) maxDist * maxDist;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                long ck = ChunkKey.of(pcx + dx, pcz + dz);
                var bucket = byChunk.get(ck);
                if (bucket == null) continue;
                for (var uuid : List.copyOf(bucket)) {
                    var crop = byUuid.get(uuid);
                    if (crop == null) continue;
                    if (!crop.getWorldName().equals(player.getWorld().getName())) continue;
                    double ddx = crop.getX() - player.getX();
                    double ddy = crop.getY() - player.getY();
                    double ddz = crop.getZ() - player.getZ();
                    if (ddx * ddx + ddy * ddy + ddz * ddz > maxDistSq) continue;
                    plugin.getFoliaScheduler().runAt(crop.getLocation(),
                            () -> spawnVisualForPlayer(crop, player));
                }
            }
        }
    }

    public Optional<UUID> getCropByEntityId(int vid) {
        return Optional.ofNullable(byEntityId.get(vid));
    }

    public void reload() {
        plugin.getSLF4JLogger().warn(
                "CropManager: config reloaded but active growth tasks and already-placed crops " +
                        "are NOT updated. A full server restart is required for structural changes.");
    }

    public void shutdown() {
        growthTasks.values().forEach(ScheduledTask::cancel);
        growthTasks.clear();
    }
}