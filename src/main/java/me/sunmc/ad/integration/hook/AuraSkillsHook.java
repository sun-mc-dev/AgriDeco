package me.sunmc.ad.integration.hook;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import me.sunmc.ad.AgriDeco;
import org.bukkit.entity.Player;

public final class AuraSkillsHook {

    private final boolean enabled;

    public AuraSkillsHook(AgriDeco plugin, boolean enabled) {
        this.enabled = enabled;
        if (enabled) plugin.getSLF4JLogger().info("AuraSkills hooked.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void grantExp(Player player, double amount) {
        if (!enabled || amount <= 0) return;
        try {
            AuraSkillsApi.get().getUser(player.getUniqueId())
                    .addSkillXp(Skills.FARMING, amount);
        } catch (Exception ignored) {
        }
    }
}