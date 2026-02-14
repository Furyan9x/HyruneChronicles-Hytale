package dev.hytalemodding.hyrune.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import dev.hytalemodding.hyrune.level.CombatXpStyle;
import dev.hytalemodding.hyrune.level.LevelingService;
import dev.hytalemodding.hyrune.skills.SkillType;
import dev.hytalemodding.hyrune.slayer.SlayerService;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * ECS system for combat xp.
 */
public class CombatXpSystem extends DeathSystems.OnDeathSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long DEFAULT_BASE_XP = 10L;
    private static final long CONSTITUTION_XP_DIVISOR = 3L;
    private static final String DEFAULT_WEAPON_ID = "bare_hands";

    private final SlayerService slayerService;

    public CombatXpSystem(SlayerService slayerService) {
        this.slayerService = slayerService;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> victimRef,
                                 @Nonnull DeathComponent deathComponent,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null) {
            return;
        }

        Damage.Source source = deathInfo.getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> killerRef = entitySource.getRef();

            if (killerRef.isValid()) {
                Player player = store.getComponent(killerRef, Player.getComponentType());
                if (player != null) {
                    handlePlayerKill(player, victimRef, store);
                }
            }
        }
    }

    private void handlePlayerKill(Player player, Ref<EntityStore> victimRef, Store<EntityStore> store) {
        long baseXp = DEFAULT_BASE_XP;

        EntityStatMap statMap = store.getComponent(victimRef, EntityStatMap.getComponentType());
        if (statMap != null) {
            try {
                int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
                EntityStatValue healthStat = statMap.get(healthIndex);
                if (healthStat != null) {
                    baseXp = (long) healthStat.getMax();
                }
            } catch (RuntimeException e) {
                LOGGER.at(Level.FINE).log("Failed to resolve health stat: " + e.getMessage());
            }
        }

        String weaponId = getHeldItemIdentifier(player);
        CombatCategory combatCategory = determineCombatCategory(weaponId);

        LevelingService service = LevelingService.get();
        if (service != null) {
            StringBuilder msg = new StringBuilder();
            awardCombatXp(service, player.getUuid(), combatCategory, baseXp, msg);

            long hpXp = Math.max(1, baseXp / CONSTITUTION_XP_DIVISOR);
            service.addSkillXp(player.getUuid(), SkillType.CONSTITUTION, hpXp);

            String victimId = getVictimIdentifier(victimRef, store);
            if (victimId != null) {
                if (slayerService.onKill(player.getUuid(), victimId, baseXp)) {
                    msg.append(" | +").append(baseXp).append(" Slayer XP");
                }
            }

            player.sendMessage(Message.raw(msg.toString()));
        }
    }

    /**
     * Determines which combat category to use based on the weapon identifier.
     */
    private CombatCategory determineCombatCategory(String weaponId) {
        String id = weaponId.toLowerCase();

        if (id.contains("bow") || id.contains("crossbow") || id.contains("gun") || id.contains("sling")) {
            return CombatCategory.RANGED;
        }

        if (id.contains("wand") || id.contains("staff") || id.contains("spellbook") || id.contains("scepter")) {
            return CombatCategory.MAGIC;
        }

        return CombatCategory.MELEE;
    }


    private String getVictimIdentifier(Ref<EntityStore> victimRef, Store<EntityStore> store) {
        try {
            NPCEntity npc = store.getComponent(victimRef, NPCEntity.getComponentType());
            if (npc != null) {
                return npc.getRoleName();
            }
        } catch (RuntimeException e) {
            LOGGER.at(Level.FINE).log("Failed to resolve NPC identifier: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves the held item ID directly from the Player object.
     */
    private String getHeldItemIdentifier(Player player) {
        if (player == null) {
            return DEFAULT_WEAPON_ID;
        }

        try {
            Inventory inventory = player.getInventory();
            if (inventory != null) {
                byte activeSlot = inventory.getActiveHotbarSlot();
                if (activeSlot >= 0 && activeSlot <= 8) {
                    ItemContainer hotbar = inventory.getHotbar();
                    if (hotbar != null) {
                        ItemStack heldStack = hotbar.getItemStack(activeSlot);
                        if (heldStack != null) {
                            Item item = heldStack.getItem();
                            if (item != null) {
                                return item.getId();
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.at(Level.WARNING).log("Error getting held item: " + e.getMessage());
        }
        return DEFAULT_WEAPON_ID;
    }

    private void awardCombatXp(LevelingService service, java.util.UUID uuid, CombatCategory category, long baseXp, StringBuilder msg) {
        if (service == null || uuid == null || msg == null || baseXp <= 0) {
            return;
        }

        switch (category) {
            case RANGED -> {
                CombatXpStyle style = service.getRangedXpStyle(uuid);
                switch (style) {
                    case DEFENCE -> {
                        service.addSkillXp(uuid, SkillType.DEFENCE, baseXp);
                        msg.append("+").append(baseXp).append(" Defence XP");
                    }
                    case SHARED -> {
                        long rangedXp = baseXp / 2;
                        long defenceXp = baseXp - rangedXp;
                        service.addSkillXp(uuid, SkillType.RANGED, rangedXp);
                        service.addSkillXp(uuid, SkillType.DEFENCE, defenceXp);
                        msg.append("+").append(rangedXp).append(" Ranged XP")
                            .append(" | +").append(defenceXp).append(" Defence XP");
                    }
                    case RANGED -> {
                        service.addSkillXp(uuid, SkillType.RANGED, baseXp);
                        msg.append("+").append(baseXp).append(" Ranged XP");
                    }
                    default -> {
                        service.addSkillXp(uuid, SkillType.RANGED, baseXp);
                        msg.append("+").append(baseXp).append(" Ranged XP");
                    }
                }
            }
            case MAGIC -> {
                CombatXpStyle style = service.getMagicXpStyle(uuid);
                switch (style) {
                    case DEFENCE -> {
                        service.addSkillXp(uuid, SkillType.DEFENCE, baseXp);
                        msg.append("+").append(baseXp).append(" Defence XP");
                    }
                    case SHARED -> {
                        long magicXp = baseXp / 2;
                        long defenceXp = baseXp - magicXp;
                        service.addSkillXp(uuid, SkillType.MAGIC, magicXp);
                        service.addSkillXp(uuid, SkillType.DEFENCE, defenceXp);
                        msg.append("+").append(magicXp).append(" Magic XP")
                            .append(" | +").append(defenceXp).append(" Defence XP");
                    }
                    case MAGIC -> {
                        service.addSkillXp(uuid, SkillType.MAGIC, baseXp);
                        msg.append("+").append(baseXp).append(" Magic XP");
                    }
                    default -> {
                        service.addSkillXp(uuid, SkillType.MAGIC, baseXp);
                        msg.append("+").append(baseXp).append(" Magic XP");
                    }
                }
            }
            case MELEE -> {
                CombatXpStyle style = service.getMeleeXpStyle(uuid);
                switch (style) {
                    case ATTACK -> {
                        service.addSkillXp(uuid, SkillType.ATTACK, baseXp);
                        msg.append("+").append(baseXp).append(" Attack XP");
                    }
                    case STRENGTH -> {
                        service.addSkillXp(uuid, SkillType.STRENGTH, baseXp);
                        msg.append("+").append(baseXp).append(" Strength XP");
                    }
                    case DEFENCE -> {
                        service.addSkillXp(uuid, SkillType.DEFENCE, baseXp);
                        msg.append("+").append(baseXp).append(" Defence XP");
                    }
                    case SHARED -> {
                        long per = baseXp / 3;
                        long remainder = baseXp - (per * 3);
                        long attackXp = per + remainder;
                        long strengthXp = per;
                        long defenceXp = per;
                        service.addSkillXp(uuid, SkillType.ATTACK, attackXp);
                        service.addSkillXp(uuid, SkillType.STRENGTH, strengthXp);
                        service.addSkillXp(uuid, SkillType.DEFENCE, defenceXp);
                        msg.append("+").append(attackXp).append(" Attack XP")
                            .append(" | +").append(strengthXp).append(" Strength XP")
                            .append(" | +").append(defenceXp).append(" Defence XP");
                    }
                    default -> {
                        service.addSkillXp(uuid, SkillType.STRENGTH, baseXp);
                        msg.append("+").append(baseXp).append(" Strength XP");
                    }
                }
            }
            default -> {
                service.addSkillXp(uuid, SkillType.STRENGTH, baseXp);
                msg.append("+").append(baseXp).append(" Strength XP");
            }
        }
    }

    private enum CombatCategory {
        MELEE,
        RANGED,
        MAGIC
    }
}
