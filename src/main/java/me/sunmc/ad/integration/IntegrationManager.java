package me.sunmc.ad.integration;

import me.sunmc.ad.AgriDeco;
import me.sunmc.ad.integration.hook.AuraSkillsHook;
import me.sunmc.ad.integration.hook.GriefPreventionHook;
import me.sunmc.ad.integration.hook.JobsHook;
import me.sunmc.ad.integration.hook.MMOItemsHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class IntegrationManager {

    private final MMOItemsHook mmoItems;
    private final AuraSkillsHook auraSkills;
    private final JobsHook jobs;
    private final GriefPreventionHook griefPrevention;

    public IntegrationManager(@NotNull AgriDeco plugin) {
        var pm = plugin.getServer().getPluginManager();
        mmoItems = new MMOItemsHook(plugin, pm.isPluginEnabled("MMOItems"));
        auraSkills = new AuraSkillsHook(plugin, pm.isPluginEnabled("AuraSkills"));
        jobs = new JobsHook(plugin, pm.isPluginEnabled("Jobs"));
        griefPrevention = new GriefPreventionHook(plugin, pm.isPluginEnabled("GriefPrevention"));

        plugin.getSLF4JLogger().info(
                "Integrations — MMOItems={} AuraSkills={} Jobs={} GriefPrevention={}",
                mmoItems.isEnabled(), auraSkills.isEnabled(),
                jobs.isEnabled(), griefPrevention.isEnabled());
    }

    @Nullable
    public ItemStack getMmoItem(String mmoId) {
        return mmoItems.getItem(mmoId);
    }

    public void grantAuraExp(Player p, double amount) {
        auraSkills.grantExp(p, amount);
    }

    public void grantJobReward(Player p, String jobId, double exp, double money) {
        jobs.grantReward(p, jobId, exp, money);
    }

    public boolean canBuild(Player p, Location loc) {
        return griefPrevention.canBuild(p, loc);
    }
}