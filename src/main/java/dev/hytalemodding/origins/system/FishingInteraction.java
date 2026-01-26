package dev.hytalemodding.origins.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import dev.hytalemodding.Origins;
import dev.hytalemodding.origins.level.LevelingService;
import dev.hytalemodding.origins.skills.SkillType;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class FishingInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<FishingInteraction> CODEC = BuilderCodec.builder(
            FishingInteraction.class,
            FishingInteraction::new,
            SimpleInstantInteraction.CODEC
        )
        .documentation("Casts or reels a fishing bobber when using a fishing rod.")
        .build();

    private static final int MAX_CAST_DISTANCE = 10;
    private static final String MODEL_ASSET_ID = "OriginsFishingBobber";
    private static final String CAST_SOUND_ID = "SFX_Origins_Fishing_Rod_Cast";
    private static final String REEL_SOUND_ID = "SFX_Origins_Fishing_Rod_Reel";

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType,
                            InteractionContext context,
                            @NonNullDecl CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        EntityStore entityStore = commandBuffer.getExternalData();
        World world = entityStore != null ? entityStore.getWorld() : null;
        if (world == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        ItemStack held = context.getHeldItem();
        if (!FishingRegistry.isFishingRod(held)) {
            return;
        }

        Ref<EntityStore> entityRef = context.getEntity();

        PlayerRef playerRef = commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());
        Player player = commandBuffer.getComponent(entityRef, Player.getComponentType());
        if (playerRef == null || player == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        playerRef.sendMessage(Message.raw("OriginsFishing: interaction triggered."));

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        FishingMetaData metaData = held != null
            ? held.getFromMetadataOrNull(FishingMetaData.KEY, FishingMetaData.CODEC)
            : null;
        if (metaData != null && metaData.getBoundBobber() != null) {
            handleReel(playerRef, inventory, metaData, commandBuffer, world);
            return;
        }

        Vector3i target = getTargetWater(world, entityRef, commandBuffer);
        if (target == null) {
            playerRef.sendMessage(Message.raw("OriginsFishing: no water target."));
            context.getState().state = InteractionState.Failed;
            return;
        }

        BlockType blockType = getBlockType(world, target);
        if (blockType == null || blockType.getId() == null) {
            playerRef.sendMessage(Message.raw("OriginsFishing: target block missing."));
            return;
        }

        Optional<FishingRegistry.BaitStack> baitOpt = FishingRegistry.findBait(inventory);
        if (baitOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("You need bait to fish."));
            return;
        }

        if (!consumeBait(inventory, baitOpt.get().itemId)) {
            playerRef.sendMessage(Message.raw("You need bait to fish."));
            return;
        }

        applyRodDurabilityLoss(player);

        LevelingService service = LevelingService.get();
        int level = service != null
            ? service.getSkillLevel(playerRef.getUuid(), SkillType.FISHING)
            : 1;

        UUID bobberId = spawnBobber(commandBuffer, target, playerRef, baitOpt.get().definition, level);
        if (bobberId != null) {
            updateRodMetadata(inventory, bobberId);
            playCastSound(playerRef);
            playerRef.sendMessage(Message.raw("You cast your line."));
        }
    }

    private static void handleReel(PlayerRef playerRef,
                                   Inventory inventory,
                                   FishingMetaData metaData,
                                   CommandBuffer<EntityStore> commandBuffer,
                                   World world) {
        Store<EntityStore> store = commandBuffer.getStore();
        UUID bobberId = metaData.getBoundBobber();
        if (bobberId == null) {
            updateRodMetadata(inventory, null);
            return;
        }

        Ref<EntityStore> bobberRef = world.getEntityStore().getRefFromUUID(bobberId);
        if (bobberRef == null || !bobberRef.isValid()) {
            updateRodMetadata(inventory, null);
            return;
        }

        FishingBobberComponent bobber = commandBuffer.getComponent(bobberRef, Origins.getFishingBobberComponentType());
        updateRodMetadata(inventory, null);
        playReelSound(playerRef);

        if (bobber == null || !bobber.canCatchFish()) {
            commandBuffer.removeEntity(bobberRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
            playerRef.sendMessage(Message.raw("Nothing is biting yet."));
            return;
        }

        LevelingService service = LevelingService.get();
        if (service == null) {
            commandBuffer.removeEntity(bobberRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
            return;
        }

        FishingRegistry.FishDefinition fish = FishingRegistry.rollFish(bobber.getFishingLevel(), bobber.getBait());
        if (fish == null) {
            commandBuffer.removeEntity(bobberRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
            playerRef.sendMessage(Message.raw("The fish got away."));
            return;
        }

        TransformComponent bobberTransform = commandBuffer.getComponent(bobberRef, TransformComponent.getComponentType());
        Vector3d catchPos = bobberTransform != null ? bobberTransform.getPosition() : null;
        ItemStack caught = new ItemStack(fish.itemId, 1);
        if (playerRef.getReference() != null) {
            com.hypixel.hytale.server.core.entity.ItemUtils.interactivelyPickupItem(
                playerRef.getReference(),
                caught,
                catchPos,
                commandBuffer
            );
        }

        service.addSkillXp(playerRef.getUuid(), SkillType.FISHING, fish.xp);
        playerRef.sendMessage(Message.raw("You caught a fish!"));
        commandBuffer.removeEntity(bobberRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
    }

    private static boolean consumeBait(Inventory inventory, String itemId) {
        ItemContainer container = inventory.getCombinedEverything();
        if (container == null) {
            return false;
        }

        ItemStack bait = new ItemStack(itemId, 1);
        ItemStackTransaction tx = container.removeItemStack(bait);
        return tx != null && tx.succeeded();
    }

    private static void applyRodDurabilityLoss(Player player) {
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }

        byte slot = inventory.getActiveHotbarSlot();
        if (slot < 0 || slot > 8) {
            return;
        }

        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar == null) {
            return;
        }

        ItemStack current = hotbar.getItemStack(slot);
        if (current == null || current.isEmpty()) {
            return;
        }

        ItemStack updated = current.withIncreasedDurability(-1);
        hotbar.setItemStackForSlot((short) slot, updated);
    }

    private static UUID spawnBobber(CommandBuffer<EntityStore> commandBuffer,
                                    Vector3i target,
                                    PlayerRef playerRef,
                                    FishingRegistry.BaitDefinition bait,
                                    int fishingLevel) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        EntityStore entityStore = commandBuffer.getExternalData();

        Vector3d position = new Vector3d(target.x + 0.5, target.y - 0.05, target.z + 0.5);
        Vector3f rotation = new Vector3f(0f, 0f, 0f);

        holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, rotation));
        holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
        holder.ensureComponent(PhysicsValues.getComponentType());

        UUID uuid = UUID.randomUUID();
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(uuid));
        holder.putComponent(NetworkId.getComponentType(), new NetworkId(entityStore.takeNextNetworkId()));

        ModelAsset asset = ModelAsset.getAssetMap().getAsset(MODEL_ASSET_ID);
        if (asset == null) {
            asset = ModelAsset.DEBUG;
        }
        Model model = Model.createRandomScaleModel(asset);

        holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
        holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));

        if (model.getBoundingBox() != null) {
            holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
        }

        holder.addComponent(Velocity.getComponentType(), new Velocity());

        FishingBobberComponent bobber = new FishingBobberComponent();
        bobber.init(playerRef.getUuid(), bait, fishingLevel);
        holder.addComponent(Origins.getFishingBobberComponentType(), bobber);

        commandBuffer.addEntity(holder, AddReason.SPAWN);
        return uuid;
    }

    private static void updateRodMetadata(Inventory inventory, UUID bobberId) {
        if (inventory == null) {
            return;
        }

        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar == null) {
            return;
        }

        byte slot = inventory.getActiveHotbarSlot();
        if (slot < 0 || slot > 8) {
            return;
        }

        ItemStack current = hotbar.getItemStack(slot);
        if (current == null) {
            return;
        }

        ItemStack updated;
        if (bobberId == null) {
            updated = current.withMetadata(FishingMetaData.KEY, null);
        } else {
            FishingMetaData metaData = current.getFromMetadataOrNull(FishingMetaData.KEY, FishingMetaData.CODEC);
            if (metaData == null) {
                metaData = new FishingMetaData();
            }
            metaData.setBoundBobber(bobberId);
            updated = current.withMetadata(FishingMetaData.KEYED_CODEC, metaData);
        }

        hotbar.replaceItemStackInSlot((short) slot, current, updated);
    }

    private static void playCastSound(PlayerRef playerRef) {
        int soundIndex = SoundEvent.getAssetMap().getIndex(CAST_SOUND_ID);
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.SFX);
    }

    private static void playReelSound(PlayerRef playerRef) {
        int soundIndex = SoundEvent.getAssetMap().getIndex(REEL_SOUND_ID);
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.SFX);
    }

    private static Vector3i getTargetWater(World world,
                                           Ref<EntityStore> entityRef,
                                           CommandBuffer<EntityStore> commandBuffer) {
        Transform look = TargetUtil.getLook(entityRef, commandBuffer);
        if (look == null) {
            return null;
        }

        Vector3d position = look.getPosition();
        Vector3d direction = look.getDirection();
        return TargetUtil.getTargetBlock(
            world,
            (blockTypeIndex, fluidTypeIndex) -> fluidTypeIndex != 0,
            position.x,
            position.y,
            position.z,
            direction.x,
            direction.y,
            direction.z,
            MAX_CAST_DISTANCE
        );
    }

    private static BlockType getBlockType(World world, Vector3i target) {
        ChunkStore chunkStore = world.getChunkStore();
        var chunkRef = chunkStore.getChunkReference(ChunkUtil.indexChunkFromBlock(target.x, target.z));
        if (chunkRef == null || !chunkRef.isValid()) {
            return null;
        }

        WorldChunk worldChunk = chunkStore.getStore().getComponent(chunkRef, WorldChunk.getComponentType());
        if (worldChunk == null) {
            return null;
        }
        return worldChunk.getBlockType(target);
    }
}
