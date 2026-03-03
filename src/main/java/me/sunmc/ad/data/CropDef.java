package me.sunmc.ad.data;

import me.sunmc.ad.util.DirectionUtil;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CropDef {

    private final List<String> stageIds;
    private final String seedId;
    private final String placementBlock;
    private final int growthTimeTicks;
    private final int growthChance;
    private final double auraExp;
    private final String jobId;
    private final double jobExp;
    private final double jobMoney;
    private final Vector armorstandOffset;
    private final boolean armorstandBaby;

    private CropDef(@NotNull Builder b) {
        stageIds = List.copyOf(b.stageIds);
        seedId = b.seedId;
        placementBlock = b.placementBlock.toUpperCase();
        growthTimeTicks = b.growthTimeTicks;
        growthChance = b.growthChance;
        auraExp = b.auraExp;
        jobId = b.jobId;
        jobExp = b.jobExp;
        jobMoney = b.jobMoney;
        armorstandOffset = DirectionUtil.parseVector(b.armorstandOffset);
        armorstandBaby = b.armorstandBaby;
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    public List<String> getStageIds() {
        return stageIds;
    }

    public String getSeedId() {
        return seedId;
    }

    public String getPlacementBlock() {
        return placementBlock;
    }

    public int getGrowthTimeTicks() {
        return growthTimeTicks;
    }

    public int getGrowthChance() {
        return growthChance;
    }

    public double getAuraExp() {
        return auraExp;
    }

    public String getJobId() {
        return jobId;
    }

    public double getJobExp() {
        return jobExp;
    }

    public double getJobMoney() {
        return jobMoney;
    }

    public @NotNull Vector getArmorstandOffset() {
        return armorstandOffset.clone();
    }

    public boolean isArmorstandBaby() {
        return armorstandBaby;
    }

    public boolean isFullyGrown(int s) {
        return s >= stageIds.size() - 1;
    }

    public static final class Builder {
        private List<String> stageIds = List.of();
        private String seedId = "";
        private String placementBlock = "farmland";
        private int growthTimeTicks = 120;
        private int growthChance = 40;
        private double auraExp = 0;
        private String jobId = "";
        private double jobExp = 0;
        private double jobMoney = 0;
        private String armorstandOffset = "0,0,0";
        private boolean armorstandBaby = false;

        public Builder stageIds(List<String> v) {
            stageIds = v;
            return this;
        }

        public Builder seedId(String v) {
            seedId = v;
            return this;
        }

        public Builder placementBlock(String v) {
            placementBlock = v;
            return this;
        }

        public Builder growthTimeTicks(int v) {
            growthTimeTicks = v;
            return this;
        }

        public Builder growthChance(int v) {
            growthChance = v;
            return this;
        }

        public Builder auraExp(double v) {
            auraExp = v;
            return this;
        }

        public Builder jobId(String v) {
            jobId = v;
            return this;
        }

        public Builder jobExp(double v) {
            jobExp = v;
            return this;
        }

        public Builder jobMoney(double v) {
            jobMoney = v;
            return this;
        }

        public Builder armorstandOffset(String v) {
            armorstandOffset = v;
            return this;
        }

        public Builder armorstandBaby(boolean v) {
            armorstandBaby = v;
            return this;
        }

        @Contract(" -> new")
        public @NotNull CropDef build() {
            return new CropDef(this);
        }
    }
}