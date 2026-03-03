package me.sunmc.ad.integration.hook;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
import me.sunmc.ad.AgriDeco;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class AuraSkillsHook {

    private final AgriDeco plugin;
    private final boolean enabled;

    public AuraSkillsHook(AgriDeco plugin, boolean enabled) {
        this.plugin = plugin;
        this.enabled = enabled;
        if (enabled) plugin.getSLF4JLogger().info("AuraSkills hooked.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void grantExp(Player player, double amount) {
        if (!enabled || amount <= 0) return;
        try {
            Skill skill = resolveSkill();
            if (skill == null) return;
            AuraSkillsApi.get().getUser(player.getUniqueId()).addSkillXp(skill, amount);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private Skill resolveSkill() {

        String name = plugin.getConfig().getString("aura_skills.skill", "farming");
        try {
            return Skills.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            try {
                return AuraSkillsApi.get().getGlobalRegistry().getSkill(NamespacedId.fromDefault(name.toLowerCase()));
            } catch (Exception ex) {
                plugin.getSLF4JLogger().warn("AuraSkills: unknown skill '{}' — XP not granted.", name);
                return null;
            }
        }
    }
}