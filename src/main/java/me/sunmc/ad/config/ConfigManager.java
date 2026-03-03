package me.sunmc.ad.config;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.data.CropDef;
import me.sunmc.ad.data.FurnitureDef;
import me.sunmc.ad.data.SoundSettings;
import me.sunmc.ad.data.enums.FurnitureType;
import me.sunmc.ad.data.enums.PlacementType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private volatile boolean removalEnabled;
    private volatile boolean removalSneakRequired;
    private volatile boolean removalOwnerOnly;
    private volatile boolean removalDropItem;

    private volatile boolean cropOwnerOnlyHarvest;

    private volatile int placementCooldownSeconds;

    private volatile SoundSettings soundPlaceFurniture;
    private volatile SoundSettings soundRemoveFurniture;
    private volatile SoundSettings soundPlantCrop;
    private volatile SoundSettings soundHarvestCrop;

    public ConfigManager(@NotNull AgriDeco plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        var cfg = plugin.getConfig();

        maxArmorStandDistance = cfg.getInt("settings.armorstands_max_distance", 32);
        chunkLimitFurnitures = cfg.getInt("settings.chunk_limit.furnitures", 32);
        chunkLimitCrops = cfg.getInt("settings.chunk_limit.crops", 64);
        reloadPermission = cfg.getString("settings.reload_permission", "agrideco.admin");

        removalEnabled = cfg.getBoolean("settings.removal.enabled", true);
        removalSneakRequired = cfg.getBoolean("settings.removal.sneak_required", true);
        removalOwnerOnly = cfg.getBoolean("settings.removal.owner_only", true);
        removalDropItem = cfg.getBoolean("settings.removal.drop_item", true);

        cropOwnerOnlyHarvest = cfg.getBoolean("settings.crops.owner_only_harvest", false);

        placementCooldownSeconds = cfg.getInt("settings.placement.cooldown_seconds", 0);

        soundPlaceFurniture = parseSound(cfg.getConfigurationSection("settings.sounds.place_furniture"), "BLOCK_WOOD_PLACE", 1.0f, 1.0f);
        soundRemoveFurniture = parseSound(cfg.getConfigurationSection("settings.sounds.remove_furniture"), "ENTITY_ITEM_BREAK", 1.0f, 1.2f);
        soundPlantCrop = parseSound(cfg.getConfigurationSection("settings.sounds.plant_crop"), "BLOCK_CROP_BREAK", 0.8f, 1.5f);
        soundHarvestCrop = parseSound(cfg.getConfigurationSection("settings.sounds.harvest_crop"), "ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f);

        furnitureDefs = parseFurniture(cfg.getConfigurationSection("furniture"));
        cropDefs = parseCrops(cfg.getConfigurationSection("crops"));

        plugin.getSLF4JLogger().info("Loaded {} furniture defs, {} crop defs.", furnitureDefs.size(), cropDefs.size());
    }

    private @NotNull SoundSettings parseSound(@Nullable ConfigurationSection sec,
                                              String defSound, float defVol, float defPitch) {
        if (sec == null) return new SoundSettings(true, defSound, defVol, defPitch);
        return new SoundSettings(
                sec.getBoolean("enabled", true),
                sec.getString("sound", defSound),
                (float) sec.getDouble("volume", defVol),
                (float) sec.getDouble("pitch", defPitch));
    }

    private Map<String, FurnitureDef> parseFurniture(@Nullable ConfigurationSection sec) {
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
                    .containerTitle(s.getString("container.title", "<dark_gray>Container"))  // Fix: MiniMessage default
                    .containerSize(s.getInt("container.size", 9))
                    .armorstandOffset(s.getString("armorstand.offset", "0,0,0"))
                    .armorstandBaby(s.getBoolean("armorstand.is_baby", false))
                    .seatOffset(s.getString("seat_offset", "0.5,0,0.5"))
                    .barrierOffsets(s.getStringList("barrier_offsets"))
                    .placePermission(s.getString("place_permission", ""))
                    .removePermission(s.getString("remove_permission", ""))
                    .removeDropItem(s.getBoolean("remove_drop_item", true))
                    .build());
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, CropDef> parseCrops(@Nullable ConfigurationSection sec) {
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
                    .placePermission(s.getString("place_permission", ""))
                    .ownerOnlyHarvest(s.getBoolean("owner_only_harvest", false))
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

    public boolean isRemovalEnabled() {
        return removalEnabled;
    }

    public boolean isRemovalSneakRequired() {
        return removalSneakRequired;
    }

    public boolean isRemovalOwnerOnly() {
        return removalOwnerOnly;
    }

    public boolean isRemovalDropItem() {
        return removalDropItem;
    }

    public boolean isCropOwnerOnlyHarvest() {
        return cropOwnerOnlyHarvest;
    }

    public int getPlacementCooldownSeconds() {
        return placementCooldownSeconds;
    }

    public SoundSettings getSoundPlaceFurniture() {
        return soundPlaceFurniture;
    }

    public SoundSettings getSoundRemoveFurniture() {
        return soundRemoveFurniture;
    }

    public SoundSettings getSoundPlantCrop() {
        return soundPlantCrop;
    }

    public SoundSettings getSoundHarvestCrop() {
        return soundHarvestCrop;
    }
}