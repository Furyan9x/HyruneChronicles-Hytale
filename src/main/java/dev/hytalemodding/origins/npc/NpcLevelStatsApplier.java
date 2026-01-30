package dev.hytalemodding.origins.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.origins.bonus.SkillStatBonusApplier;

public final class NpcLevelStatsApplier {
    private static final String HEALTH_ID = "Health";
    private static final String HEALTH_MODIFIER_ID = "origins:npc_health_bonus";

    private NpcLevelStatsApplier() {
    }

    public static void apply(Store<EntityStore> store, Ref<EntityStore> ref, NpcLevelComponent component) {
        if (store == null || ref == null || component == null) {
            return;
        }

        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }

        int healthIndex = getStatIndex(statMap, HEALTH_ID);
        if (healthIndex < 0) {
            return;
        }

        float healthBonus = component.getLevel() * SkillStatBonusApplier.HEALTH_PER_CONSTITUTION;
        Modifier modifier = new StaticModifier(Modifier.ModifierTarget.MAX,
            StaticModifier.CalculationType.ADDITIVE, healthBonus);
        statMap.putModifier(healthIndex, HEALTH_MODIFIER_ID, modifier);
    }

    private static int getStatIndex(EntityStatMap statMap, String statId) {
        if (statMap == null || statId == null) {
            return -1;
        }

        var statValue = statMap.get(statId);
        return statValue != null ? statValue.getIndex() : -1;
    }
}
