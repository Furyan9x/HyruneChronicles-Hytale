package dev.hytalemodding.hyrune.bonus;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.Hyrune;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.tradepack.TradePackUtils;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Applies skill stat bonuses.
 */
public final class SkillStatBonusApplier {
    public static final float HEALTH_PER_CONSTITUTION = 5.0f;
    public static final float MANA_MAX_PER_MAGIC = 7.0f;
    public static final float STAMINA_MAX_PER_AGILITY = 40.0f / 99.0f;
    public static final float MOVEMENT_SPEED_BONUS_AT_99 = 0.35f;

    public static final float MANA_REGEN_PER_MAGIC = 0.2f;
    public static final float STAMINA_REGEN_PER_AGILITY = 1.0f / 99.0f;

    private static final String HEALTH_MODIFIER_ID = "hyrune:health_bonus";
    private static final String MANA_MAX_MODIFIER_ID = "hyrune:mana_max_bonus";
    private static final String STAMINA_MAX_MODIFIER_ID = "hyrune:stamina_max_bonus";

    private static final String HEALTH_ID = "Health";
    private static final String MANA_ID = "Mana";
    private static final String STAMINA_ID = "Stamina";

    private SkillStatBonusApplier() {
    }

    public static void apply(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        apply(playerRef.getUuid(), store.getComponent(ref, EntityStatMap.getComponentType()));
    }

    public static void applyMovementSpeed(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        MovementManager movementManager = store.getComponent(ref, MovementManager.getComponentType());
        if (movementManager == null) {
            return;
        }

        LevelingService service = Hyrune.getService();
        int agility = service.getSkillLevel(playerRef.getUuid(), SkillType.AGILITY);
        float bonus = (agility / 99.0f) * MOVEMENT_SPEED_BONUS_AT_99;

        float defaultBaseSpeed = movementManager.getDefaultSettings().baseSpeed;
        if (defaultBaseSpeed < 0.1f) {
            defaultBaseSpeed = 5.5f;
        }

        float packMultiplier = TradePackUtils.hasTradePack(playerRef)
            ? TradePackUtils.TRADE_PACK_SPEED_MULTIPLIER
            : 1.0f;
        movementManager.getSettings().baseSpeed = defaultBaseSpeed * (1.0f + bonus) * packMultiplier;
        if (playerRef.getPacketHandler() != null) {
            movementManager.update(playerRef.getPacketHandler());
        }
    }

    public static void apply(@Nullable Holder<EntityStore> holder, @Nullable UUID uuid) {
        if (holder == null || uuid == null) {
            return;
        }

        apply(uuid, holder.getComponent(EntityStatMap.getComponentType()));
    }

    private static void apply(UUID uuid, @Nullable EntityStatMap statMap) {
        if (statMap == null) {
            return;
        }

        LevelingService service = Hyrune.getService();
        int constitution = service.getSkillLevel(uuid, SkillType.CONSTITUTION);
        int magic = service.getSkillLevel(uuid, SkillType.MAGIC);
        int agility = service.getSkillLevel(uuid, SkillType.AGILITY);

        int healthIndex = getStatIndex(statMap, HEALTH_ID);
        if (healthIndex >= 0) {
            float healthBonus = constitution * HEALTH_PER_CONSTITUTION;
            Modifier healthModifier = new StaticModifier(Modifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.ADDITIVE, healthBonus);
            statMap.putModifier(healthIndex, HEALTH_MODIFIER_ID, healthModifier);
        }

        int manaIndex = getStatIndex(statMap, MANA_ID);
        if (manaIndex >= 0) {
            float manaBonus = magic * MANA_MAX_PER_MAGIC;
            Modifier manaModifier = new StaticModifier(Modifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.ADDITIVE, manaBonus);
            statMap.putModifier(manaIndex, MANA_MAX_MODIFIER_ID, manaModifier);
        }

        int staminaIndex = getStatIndex(statMap, STAMINA_ID);
        if (staminaIndex >= 0) {
            float staminaBonus = agility * STAMINA_MAX_PER_AGILITY;
            Modifier staminaModifier = new StaticModifier(Modifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.ADDITIVE, staminaBonus);
            statMap.putModifier(staminaIndex, STAMINA_MAX_MODIFIER_ID, staminaModifier);
        }
    }

    private static int getStatIndex(EntityStatMap statMap, String statId) {
        if (statMap == null || statId == null) {
            return -1;
        }

        var statValue = statMap.get(statId);
        return statValue != null ? statValue.getIndex() : -1;
    }
}
