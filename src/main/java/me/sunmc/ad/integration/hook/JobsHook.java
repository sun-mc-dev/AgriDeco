package me.sunmc.ad.integration.hook;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.CurrencyType;
import me.sunmc.ad.AgriDeco;
import org.bukkit.entity.Player;

import java.util.Map;

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

            var prog = jp.getJobProgression(job);
            if (prog != null) prog.addExperience(exp);

            Jobs.getEconomy().pay(jp, Map.of(CurrencyType.MONEY, money));
        } catch (Exception ignored) {
        }
    }
}