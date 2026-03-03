package me.sunmc.ad.database;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.data.PlacedCrop;
import me.sunmc.ad.data.PlacedFurniture;
import me.sunmc.ad.packet.VirtualEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class DataRepository {

    private static final String SELECT_FURNITURE =
            "SELECT uuid, config_id, world, x, y, z, yaw, owner, state FROM agrideco_furniture";
    private static final String SELECT_CROPS =
            "SELECT uuid, config_id, world, x, y, z, stage, owner FROM agrideco_crops";

    private final AgriDeco plugin;
    private final DatabaseManager db;
    private final Executor io = Executors.newVirtualThreadPerTaskExecutor();

    public DataRepository(@NotNull AgriDeco plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    private boolean isSqlite() {
        return db.getStorageType() == DatabaseManager.StorageType.SQLITE;
    }

    @Contract(pure = true)
    private @NotNull String furnitureUpsert() {
        return isSqlite()
                ? "INSERT OR REPLACE INTO agrideco_furniture (uuid,config_id,world,x,y,z,yaw,owner,state) VALUES (?,?,?,?,?,?,?,?,?)"
                : "INSERT INTO agrideco_furniture (uuid,config_id,world,x,y,z,yaw,owner,state) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE state=VALUES(state),yaw=VALUES(yaw)";
    }

    @Contract(pure = true)
    private @NotNull String cropUpsert() {
        return isSqlite()
                ? "INSERT OR REPLACE INTO agrideco_crops (uuid,config_id,world,x,y,z,stage,owner) VALUES (?,?,?,?,?,?,?,?)"
                : "INSERT INTO agrideco_crops (uuid,config_id,world,x,y,z,stage,owner) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE stage=VALUES(stage)";
    }

    @Contract("_ -> new")
    public @NotNull CompletableFuture<Void> saveFurniture(PlacedFurniture f) {
        return CompletableFuture.runAsync(() -> {
            try (var c = db.getConnection();
                 var ps = c.prepareStatement(furnitureUpsert())) {
                var l = f.getLocation();
                ps.setString(1, f.getUuid().toString());
                ps.setString(2, f.getConfigId());
                ps.setString(3, Objects.requireNonNull(l.getWorld()).getName());
                ps.setDouble(4, l.getX());
                ps.setDouble(5, l.getY());
                ps.setDouble(6, l.getZ());
                ps.setFloat(7, l.getYaw());
                ps.setString(8, f.getOwnerUuid().toString());
                ps.setInt(9, f.getInteractionState());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("saveFurniture failed: {}", f.getUuid(), e);
            }
        }, io);
    }

    @Contract("_ -> new")
    public @NotNull CompletableFuture<Void> deleteFurniture(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (var c = db.getConnection();
                 var ps = c.prepareStatement("DELETE FROM agrideco_furniture WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("deleteFurniture failed: {}", uuid, e);
            }
        }, io);
    }

    @Contract(" -> new")
    public @NotNull CompletableFuture<List<PlacedFurniture>> loadAllFurniture() {
        return CompletableFuture.supplyAsync(() -> {
            var list = new ArrayList<PlacedFurniture>();
            try (var c = db.getConnection();
                 var st = c.createStatement();
                 var rs = st.executeQuery(SELECT_FURNITURE)) {
                while (rs.next()) {
                    var world = Bukkit.getWorld(rs.getString("world"));
                    if (world == null) continue;
                    var loc = new Location(world,
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), 0f);
                    var pf = new PlacedFurniture(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("config_id"), loc,
                            UUID.fromString(rs.getString("owner")),
                            VirtualEntity.nextId());
                    pf.setInteractionState(rs.getInt("state"));
                    list.add(pf);
                }
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("loadAllFurniture failed", e);
                throw new RuntimeException(e);
            }
            return list;
        }, io).exceptionally(ex -> {
            plugin.getSLF4JLogger().error("loadAllFurniture — returning empty list due to error", ex);
            return List.of();
        });
    }

    @Contract("_ -> new")
    public @NotNull CompletableFuture<Void> saveCrop(PlacedCrop crop) {
        return CompletableFuture.runAsync(() -> {
            try (var c = db.getConnection();
                 var ps = c.prepareStatement(cropUpsert())) {
                var l = crop.getLocation();
                ps.setString(1, crop.getUuid().toString());
                ps.setString(2, crop.getConfigId());
                ps.setString(3, Objects.requireNonNull(l.getWorld()).getName());
                ps.setDouble(4, l.getX());
                ps.setDouble(5, l.getY());
                ps.setDouble(6, l.getZ());
                ps.setInt(7, crop.getStage());
                ps.setString(8, crop.getOwnerUuid().toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("saveCrop failed: {}", crop.getUuid(), e);
            }
        }, io);
    }

    @Contract("_ -> new")
    public @NotNull CompletableFuture<Void> deleteCrop(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (var c = db.getConnection();
                 var ps = c.prepareStatement("DELETE FROM agrideco_crops WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("deleteCrop failed: {}", uuid, e);
            }
        }, io);
    }

    @Contract(" -> new")
    public @NotNull CompletableFuture<List<PlacedCrop>> loadAllCrops() {
        return CompletableFuture.supplyAsync(() -> {
            var list = new ArrayList<PlacedCrop>();
            try (var c = db.getConnection();
                 var st = c.createStatement();
                 var rs = st.executeQuery(SELECT_CROPS)) {
                while (rs.next()) {
                    var world = Bukkit.getWorld(rs.getString("world"));
                    if (world == null) continue;
                    var loc = new Location(world,
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
                    var crop = new PlacedCrop(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("config_id"), loc,
                            UUID.fromString(rs.getString("owner")),
                            VirtualEntity.nextId());
                    crop.setStage(rs.getInt("stage"));
                    list.add(crop);
                }
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("loadAllCrops failed", e);
                throw new RuntimeException(e);
            }
            return list;
        }, io).exceptionally(ex -> {
            plugin.getSLF4JLogger().error("loadAllCrops — returning empty list due to error", ex);
            return List.of();
        });
    }
}