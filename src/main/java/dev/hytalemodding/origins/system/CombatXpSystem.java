package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
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
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import dev.hytalemodding.origins.slayer.SlayerService;

import javax.annotation.Nonnull;

public class CombatXpSystem extends DeathSystems.OnDeathSystem {

    public final SlayerService slayerService;

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
        long baseXp = 10;

        EntityStatMap statMap = store.getComponent(victimRef, EntityStatMap.getComponentType());
        if (statMap != null) {
            try {
                int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
                EntityStatValue healthStat = statMap.get(healthIndex);
                if (healthStat != null) {
                    baseXp = (long) healthStat.getMax();
                }
            } catch (Exception e) {
                // Ignore missing stats
            }
        }

        String weaponId = getHeldItemIdentifier(player);
        SkillType targetSkill = determineSkillFromWeapon(weaponId);

        LevelingService service = LevelingService.get();
        if (service != null) {
            StringBuilder msg = new StringBuilder();
            service.addSkillXp(player.getUuid(), targetSkill, baseXp);

            long hpXp = Math.max(1, baseXp / 3);
            service.addSkillXp(player.getUuid(), SkillType.CONSTITUTION, hpXp);

            player.sendMessage(Message.raw("+" + baseXp + " " + targetSkill.getDisplayName() + " XP"));

            String victimId = getVictimIdentifier(victimRef, store);
            if (victimId != null) {
                if (slayerService.onKill(player.getUuid(), victimId, baseXp)) {
                    msg.append(" | +").append(baseXp).append(" Slayer XP");
                }
            }

            player.sendMessage(Message.raw(msg.toString()));
        }
    }
    private String getVictimIdentifier(Ref<EntityStore> victimRef, Store<EntityStore> store) {
        try {
            NPCEntity npc = store.getComponent(victimRef, NPCEntity.getComponentType());
            if (npc != null) {
                return npc.getRoleName();
            }
        } catch (Exception ignored) {
            // Log if necessary
        }
        return null;
    }


/**
 * Determines which skill to level based on the weapon identifier.
 */
private SkillType determineSkillFromWeapon(String weaponId) {
    String id = weaponId.toLowerCase();

    if (id.contains("bow") || id.contains("crossbow") || id.contains("gun") || id.contains("sling")) {
        return SkillType.RANGED;
    }

    if (id.contains("wand") || id.contains("staff") || id.contains("spellbook") || id.contains("scepter")) {
        return SkillType.MAGIC;
    }

    // Melee Logic
    if (id.contains("sword") || id.contains("dagger") || id.contains("scythe")) {
        return SkillType.ATTACK;
    }

    if (id.contains("axe") || id.contains("mace") || id.contains("hammer") || id.contains("club")) {
        return SkillType.STRENGTH;
    }

    if (id.contains("shield")) {
        return SkillType.DEFENCE;
    }

    // Unarmed / Default
    return SkillType.STRENGTH;
}

/**
 * Retrieves the held item ID directly from the Player object.
 * Adapted from RequirementChecker.java
 */
private String getHeldItemIdentifier(Player player) {
    if (player == null) {
        return "bare_hands";
    }

    try {
        Inventory inventory = player.getInventory();
        if (inventory != null) {
            // 1. Check Active Hotbar Slot (0-8)
            byte activeSlot = inventory.getActiveHotbarSlot();
            if (activeSlot >= 0 && activeSlot <= 8) {
                ItemContainer hotbar = inventory.getHotbar();
                if (hotbar != null) {
                    ItemStack heldStack = hotbar.getItemStack(activeSlot);
                    if (heldStack != null) {
                        Item item = heldStack.getItem();
                        if (item != null) {
                            return item.getId(); // e.g. "sword_iron"
                        }
                    }
                }
            }
        }
    } catch (Exception e) {
        System.err.println("[Origins] Error getting held item: " + e.getMessage());
    }
    return "bare_hands";
}
}
