package me.sunmc.ad.integration.hook;

import com.gamingmesh.jobs.Jobs;
import me.sunmc.ad.AgriDeco;
import org.bukkit.entity.Player;

public final class JobsHook {

    private final boolean enabled;

    public JobsHook(AgriDeco plugin, boolean enabled) {
        this.enabled = enabled;
        if (enabled) plugin.getSLF4JLogger().info("JobsReborn hooked.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void grantReward(Player player, String jobId, double exp, double money) {
        if (!enabled || jobId == null || jobId.isBlank()) return;
        try {
            var jp = Jobs.getPlayerManager().getJobsPlayer(player);
            var job = Jobs.getJob(jobId);
            if (jp == null || job == null) return;
            jp.addExperience(job, exp);
            Jobs.getEconomy().pay(jp, money);
        } catch (Exception ignored) {
        }
    }
}