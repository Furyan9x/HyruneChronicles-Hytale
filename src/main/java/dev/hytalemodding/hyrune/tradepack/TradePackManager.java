package dev.hytalemodding.hyrune.tradepack;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import dev.hytalemodding.hyrune.bonus.SkillStatBonusApplier;
import dev.hytalemodding.hyrune.util.PlayerEntityAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for trade pack.
 */
public final class TradePackManager {
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    private TradePackManager() {
    }

    public static void sync(Player player) {
        if (player == null) {
            return;
        }
        boolean hasTradePack = TradePackUtils.hasTradePack(player);
        UUID uuid = PlayerEntityAccess.getPlayerUuid(player);
        if (uuid == null) {
            return;
        }
        boolean wasActive = ACTIVE.contains(uuid);
        if (hasTradePack == wasActive) {
            return;
        }

        if (hasTradePack) {
            ACTIVE.add(uuid);
        } else {
            ACTIVE.remove(uuid);
        }

        applyVisuals(player, hasTradePack);
        PlayerRef playerRef = PlayerEntityAccess.getPlayerRef(player);
        if (playerRef != null) {
            SkillStatBonusApplier.applyMovementSpeed(playerRef);
        }
    }


    public static void clear(UUID uuid) {
        if (uuid != null) {
            ACTIVE.remove(uuid);
        }
    }

    private static void applyVisuals(Player player, boolean hasTradePack) {
        PlayerRef playerRef = PlayerEntityAccess.getPlayerRef(player);
        if (playerRef == null) {
            return;
        }

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        setBackpackAttachment(ref, store, hasTradePack);
    }

    private static void setBackpackAttachment(Ref<EntityStore> ref,
                                              Store<EntityStore> store,
                                              boolean hasTradePack) {
        ModelComponent modelComponent = store.getComponent(ref, ModelComponent.getComponentType());
        if (modelComponent == null) {
            return;
        }

        Model model = modelComponent.getModel();
        if (model == null) {
            return;
        }

        List<ModelAttachment> attachments = new ArrayList<>();
        if (model.getAttachments() != null) {
            for (ModelAttachment attachment : model.getAttachments()) {
                if (attachment != null && !isTradePackAttachment(attachment)) {
                    attachments.add(attachment);
                }
            }
        }


        if (hasTradePack) {
            attachments.add(new ModelAttachment(
                TradePackUtils.TRADE_PACK_MODEL,
                TradePackUtils.TRADE_PACK_TEXTURE,
                null,
                null,
                1.0
            ));
        }

        Model updated = new Model(
            model.getModelAssetId(),
            model.getScale(),
            model.getRandomAttachmentIds(),
            attachments.toArray(new ModelAttachment[0]),
            model.getBoundingBox(),
            model.getModel(),
            model.getTexture(),
            model.getGradientSet(),
            model.getGradientId(),
            model.getEyeHeight(),
            model.getCrouchOffset(),
            model.getSittingOffset(),
            model.getSleepingOffset(),
            model.getAnimationSetMap(),
            model.getCamera(),
            model.getLight(),
            model.getParticles(),
            model.getTrails(),
            model.getPhysicsValues(),
            model.getDetailBoxes(),
            model.getPhobia(),
            model.getPhobiaModelAssetId()
        );
        store.putComponent(ref, ModelComponent.getComponentType(), new ModelComponent(updated));

    }
    private static boolean isTradePackAttachment(ModelAttachment attachment) {
        return TradePackUtils.TRADE_PACK_MODEL.equals(attachment.getModel())
            && TradePackUtils.TRADE_PACK_TEXTURE.equals(attachment.getTexture());
    }
}
