package dev.hytalemodding.origins.events;

import dev.hytalemodding.origins.skills.SkillType;

import java.util.UUID;

/**
 * Contract for xp gain listener.
 */
public interface XpGainListener {
    /**
     * Called after XP is awarded to a skill.
     */
    void onXpGain(UUID uuid, long amount, SkillType skill);
}
