package dev.hytalemodding.origins.events;
import dev.hytalemodding.origins.skills.SkillType;

import java.util.UUID;

public interface XpGainListener {
    // Updated to accept the SkillType
    void onXpGain(UUID uuid, long amount, SkillType skill);
}