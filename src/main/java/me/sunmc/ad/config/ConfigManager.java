package me.sunmc.ad.config;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.data.CropDef;
import me.sunmc.ad.data.FurnitureDef;
import me.sunmc.ad.data.enums.FurnitureType;
import me.sunmc.ad.data.enums.PlacementType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ConfigManager {

    private final AgriDeco plugin;

    private volatile Map<String, FurnitureDef> furnitureDefs = Collections.emptyMap();
    private volatile Map<String, CropDef> cropDefs = Collections.emptyMap();

    private volatile int maxArmorStandDistance;
    private volatile int chunkLimitFurnitures;
    private volatile int chunkLimitCrops;
    private volatile String reloadPermission;

    public ConfigManager(@NotNull AgriDeco plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        var cfg = plugin.getConfig();
        maxArmorStandDistance = cfg.getInt("settings.armorstands_max_distance", 32);
        chunkLimitFurnitures = cfg.getInt("settings.chunk_limit.furnitures", 32);
        chunkLimitCrops = cfg.getInt("settings.chunk_limit.crops", 64);
        reloadPermission = cfg.getString("settings.reload_permission", "agrideco.admin");
        furnitureDefs = parseFurniture(cfg.getConfigurationSection("furniture"));
        cropDefs = parseCrops(cfg.getConfigurationSection("crops"));

        plugin.getSLF4JLogger().info("Loaded {} furniture defs, {} crop defs.",
                furnitureDefs.size(), cropDefs.size());
    }

    private Map<String, FurnitureDef> parseFurniture(ConfigurationSection sec) {
        if (sec == null) return Collections.emptyMap();
        var out = new HashMap<String, FurnitureDef>();
        for (String key : sec.getKeys(false)) {
            var s = sec.getConfigurationSection(key);
            if (s == null) continue;
            out.put(key, FurnitureDef.builder()
                    .id(s.getString("id", ""))
                    .placementType(PlacementType.fromString(s.getString("placement_type", "floor")))
                    .furnitureType(FurnitureType.fromString(s.getString("furniture_type", "decorative")))
                    .interactionId(s.getString("interaction_mmoitem_id", ""))
                    .containerTitle(s.getString("container.title", "&8Container"))
                    .containerSize(s.getInt("container.size", 9))
                    .armorstandOffset(s.getString("armorstand.offset", "0,0,0"))
                    .armorstandBaby(s.getBoolean("armorstand.is_baby", false))
                    .seatOffset(s.getString("seat_offset", "0.5,0,0.5"))
                    .barrierOffsets(s.getStringList("barrier_offsets"))
                    .build());
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, CropDef> parseCrops(ConfigurationSection sec) {
        if (sec == null) return Collections.emptyMap();
        var out = new HashMap<String, CropDef>();
        for (String key : sec.getKeys(false)) {
            var s = sec.getConfigurationSection(key);
            if (s == null) continue;
            out.put(key, CropDef.builder()
                    .stageIds(s.getStringList("crop_stage_ids"))
                    .seedId(s.getString("seed_id", ""))
                    .placementBlock(s.getString("placement_block_type", "farmland"))
                    .growthTimeTicks(s.getInt("growth_time", 120))
                    .growthChance(s.getInt("growth_chance", 40))
                    .auraExp(s.getDouble("aura_skills.experience", 0))
                    .jobId(s.getString("jobs.job_id", ""))
                    .jobExp(s.getDouble("jobs.experience", 0))
                    .jobMoney(s.getDouble("jobs.money", 0))
                    .armorstandOffset(s.getString("armorstand.offset", "0,0,0"))
                    .armorstandBaby(s.getBoolean("armorstand.is_baby", false))
                    .build());
        }
        return Collections.unmodifiableMap(out);
    }

    public Map<String, FurnitureDef> getFurnitureDefs() {
        return furnitureDefs;
    }

    public Map<String, CropDef> getCropDefs() {
        return cropDefs;
    }

    public int getMaxArmorStandDistance() {
        return maxArmorStandDistance;
    }

    public int getChunkLimitFurnitures() {
        return chunkLimitFurnitures;
    }

    public int getChunkLimitCrops() {
        return chunkLimitCrops;
    }

    public String getReloadPermission() {
        return reloadPermission;
    }
}