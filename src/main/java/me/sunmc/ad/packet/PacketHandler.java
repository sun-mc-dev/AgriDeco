package me.sunmc.ad.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.sunmc.ad.AgriDeco;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PacketHandler extends PacketListenerAbstract {

    private final AgriDeco plugin;
    private final ConcurrentHashMap<Integer, Consumer<Player>> clickHandlers = new ConcurrentHashMap<>();

    public PacketHandler(AgriDeco plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        var wrap = new WrapperPlayClientInteractEntity(event);
        if (wrap.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;
        var handler = clickHandlers.get(wrap.getEntityId());
        if (handler == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        plugin.getFoliaScheduler().runAt(player.getLocation(), () -> handler.accept(player));
    }

    @Contract("_, _, _, _, _ -> new")
    private PacketWrapper<?> @NotNull [] buildSpawnPackets(int entityId, @NotNull Location loc,
                                                           ItemStack headItem,
                                                           boolean small, boolean invisible) {
        var spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.ARMOR_STAND,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                loc.getPitch(), loc.getYaw(), loc.getYaw(),
                0, Optional.empty());

        List<EntityData<?>> meta = List.of(
                new EntityData<>(0,  EntityDataTypes.BYTE, (byte) (invisible ? 0x20 : 0x00)),
                new EntityData<>(15, EntityDataTypes.BYTE, (byte) ((small ? 0x01 : 0x00) | 0x08 | 0x10))
        );
        var metaPkt = new WrapperPlayServerEntityMetadata(entityId, meta);

        var peItem = SpigotConversionUtil.fromBukkitItemStack(headItem);
        var equipPkt = new WrapperPlayServerEntityEquipment(
                entityId, List.of(new Equipment(EquipmentSlot.HELMET, peItem)));

        return new PacketWrapper[]{spawn, metaPkt, equipPkt};
    }

    public void spawnArmorStand(int entityId, Location location, ItemStack headItem,
                                boolean small, boolean invisible, Consumer<Player> onClick) {
        if (onClick != null) clickHandlers.put(entityId, onClick);
        broadcast(location, buildSpawnPackets(entityId, location, headItem, small, invisible));
    }

    public void spawnArmorStandForPlayer(Player player, int entityId, Location location,
                                         ItemStack headItem, boolean small, boolean invisible) {
        sendTo(player, buildSpawnPackets(entityId, location, headItem, small, invisible));
    }

    public void despawnEntity(int entityId, Location location) {
        clickHandlers.remove(entityId);
        broadcast(location, new WrapperPlayServerDestroyEntities(entityId));
    }

    public void sendAllEntitiesToPlayer(Player player) {
        var fm = plugin.getFurnitureManager();
        var cm = plugin.getCropManager();
        if (fm != null) fm.sendVisibleFurnitureTo(player);
        if (cm != null) cm.sendVisibleCropsTo(player);
    }

    private void broadcast(@NotNull Location loc, PacketWrapper<?>... packets) {
        int dist = plugin.getConfigManager().getMaxArmorStandDistance();
        long distSq = (long) dist * dist;
        for (var p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(loc) > distSq) continue;
            sendTo(p, packets);
        }
    }

    private void sendTo(Player player, PacketWrapper<?> @NotNull ... packets) {
        var mgr = PacketEvents.getAPI().getPlayerManager();
        for (var pkt : packets) mgr.sendPacket(player, pkt);
    }

    public void shutdown() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        clickHandlers.clear();
    }
}